package com.aliucord.coreplugins

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Constants
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.*
import com.aliucord.coreplugins.componentsv2.patchMessageItems
import com.aliucord.coreplugins.componentsv2.views.*
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.utils.ReflectUtils
import com.discord.api.botuikit.*
import com.discord.api.botuikit.gson.ComponentRuntimeTypeAdapter
import com.discord.api.botuikit.gson.ComponentTypeTypeAdapter
import com.discord.api.message.attachment.MessageAttachment
import com.discord.models.botuikit.*
import com.discord.models.message.Message
import com.discord.stores.StoreApplicationInteractions.InteractionSendState
import com.discord.utilities.view.extensions.ViewExtensions
import com.discord.widgets.botuikit.*
import com.discord.widgets.botuikit.ComponentChatListState.ComponentStoreState
import com.discord.widgets.botuikit.views.*
import com.discord.widgets.botuikit.views.select.SelectComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.discord.widgets.chat.list.entries.BotUiComponentEntry
import com.google.gson.stream.JsonReader
import com.lytefast.flexinput.R
import de.robv.android.xposed.XposedBridge
import java.io.File

val Message.isComponentV2 get() = (flags shr 15) and 1 == 1L

@AliucordPlugin(requiresRestart = true)
@Suppress("unused")
class ComponentsV2 : Plugin() {
    companion object {
        /** Creates a new [MessageAttachment] */
        fun createAttachment(
            filename: String,
            filesize: Long,
            proxyUrl: String,
            url: String,
            width: Int,
            height: Int,
        ): MessageAttachment {
            val inst = ReflectUtils.allocateInstance(clazz)
            filenameField.set(inst, filename)
            filesizeField.set(inst, filesize)
            proxyUrlField.set(inst, proxyUrl)
            urlField.set(inst, url)
            widthField.set(inst, width)
            heightField.set(inst, height)
            return inst
        }

        private val clazz = MessageAttachment::class.java
        private val filenameField = clazz.getDeclaredField("filename").apply { isAccessible = true }
        private val filesizeField = clazz.getDeclaredField("size").apply { isAccessible = true }
        private val proxyUrlField = clazz.getDeclaredField("proxyUrl").apply { isAccessible = true }
        private val urlField = clazz.getDeclaredField("url").apply { isAccessible = true }
        private val widthField = clazz.getDeclaredField("width").apply { isAccessible = true }
        private val heightField = clazz.getDeclaredField("height").apply { isAccessible = true }
    }

    override fun start(context: Context) {
        val oldFile = File("${Constants.PLUGINS_PATH}/ComponentsV2-Beta.zip")
        if (oldFile.exists()) {
            logger.info("old plugin found, deleting and prompting restart")
            oldFile.delete()
            Utils.promptRestart()
            return
        }

        XposedBridge.makeClassInheritable(BotUiComponentEntry::class.java)
        ComponentV2Type.make()
        patchGson()
        // https://github.com/LSPosed/LSPlant/issues/41
        patchMessageItems(patcher)

        patcher.instead<ComponentStateMapper>(
            "toMessageLayoutComponent",
            LayoutComponent::class.java,
            Int::class.javaPrimitiveType!!,
            List::class.java,
            ComponentExperiments::class.java
        ) { (_, layout: LayoutComponent, index: Int, components: List<MessageComponent>) ->
            when (layout) {
                is ActionRowComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is SectionComponent ->
                    SectionMessageComponent.mergeToMessageComponent(layout, index, components)
                is TextDisplayComponent ->
                    TextDisplayMessageComponent.mergeToMessageComponent(layout, index)
                is ThumbnailComponent ->
                    ThumbnailMessageComponent.mergeToMessageComponent(layout, index)
                is MediaGalleryComponent ->
                    MediaGalleryMessageComponent.mergeToMessageComponent(layout, index)
                is FileComponent ->
                    ActionRowMessageComponent(layout.type, index, components)
                is SeparatorComponent ->
                    SeparatorMessageComponent.mergeToMessageComponent(layout, index)
                is ContainerComponent ->
                    ContainerMessageComponent.mergeToMessageComponent(layout, index, components)
                else ->
                    throw IllegalArgumentException("Unknown layout component ${layout::class.java.name} (${layout.type.type}:${layout.type.name})")
            }
        }

        patcher.instead<ComponentProvider>("configureView", ComponentActionListener::class.java, MessageComponent::class.java, ComponentView::class.java)
        { (_, listener: ComponentActionListener, component: MessageComponent, view: ComponentView<MessageComponent>?) ->
            view?.configure(component, this, listener)
        }

        patcher.instead<ComponentInflater>("inflateComponent", ComponentType::class.java, ViewGroup::class.java)
        { (_, type: ComponentType, viewGroup: ViewGroup) ->
            when (type) {
                ComponentType.ACTION_ROW ->
                    ActionRowComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentType.BUTTON ->
                    ButtonComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentType.SELECT ->
                    SelectComponentView.Companion!!.inflateComponent(this.context, viewGroup)
                ComponentV2Type.USER_SELECT,
                ComponentV2Type.ROLE_SELECT,
                ComponentV2Type.MENTIONABLE_SELECT,
                ComponentV2Type.CHANNEL_SELECT ->
                    SelectV2ComponentView(this.context, type)
                ComponentV2Type.SECTION ->
                    SectionComponentView(this.context)
                ComponentV2Type.TEXT_DISPLAY ->
                    TextDisplayComponentView(this.context)
                ComponentV2Type.THUMBNAIL ->
                    ThumbnailComponentView(this.context)
                ComponentV2Type.MEDIA_GALLERY ->
                    MediaGalleryComponentView(this.context)
                ComponentV2Type.FILE ->
                    null
                ComponentV2Type.SEPARATOR ->
                    SeparatorComponentView(this.context)
                ComponentV2Type.CONTAINER ->
                    ContainerComponentView(this.context)
                else -> null
            }
        }

        patcher.after<WidgetChatListAdapterItemBotComponentRow>(WidgetChatListAdapter::class.java)
        {
            val rootLayout = itemView.findViewById<LinearLayout>(Utils.getResId("chat_list_adapter_item_component_root", "id"))
            rootLayout.layoutParams = (rootLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                marginEnd = adapter.context.resources.getDimension(R.d.chat_cell_horizontal_spacing_padding).toInt()
            }

            ViewExtensions.setOnLongClickListenerConsumeClick(itemView) {
                adapter.eventHandler.onMessageLongClicked(entry.message, "", false)
            }
            itemView.setOnClickListener {
                adapter.eventHandler.onMessageClicked(entry.message, false)
            }
        }

        patcher.instead<ComponentStateMapper>(
            "createActionMessageComponent",
            ActionComponent::class.java,
            Int::class.javaPrimitiveType!!,
            ComponentStoreState::class.java,
            ComponentExperiments::class.java,
        ) { (
            _,
            actionComponent: ActionComponent,
            index: Int,
            componentStoreState: ComponentStoreState,
        ) ->
            val interactionState: Map<Int, InteractionSendState>? = componentStoreState.interactionState;
            val num = interactionState?.entries?.find { it.value is InteractionSendState.Loading }?.key

            val state = interactionState?.get(index)
            val comState: ActionInteractionComponentState = when {
                state is InteractionSendState.Failed -> ActionInteractionComponentState.Failed(state.errorMessage)
                num == null -> ActionInteractionComponentState.Enabled.INSTANCE
                num == index -> ActionInteractionComponentState.Loading.INSTANCE
                else -> ActionInteractionComponentState.Disabled.INSTANCE
            }

            when (actionComponent) {
                is ButtonComponent ->
                    ButtonMessageComponentKt.mergeToMessageComponent(actionComponent, index, comState, componentStoreState)
                is SelectComponent ->
                    SelectMessageComponentKt.mergeToMessageComponent(actionComponent, index, comState, componentStoreState)
                is SelectV2Component ->
                    SelectV2MessageComponent.mergeToMessageComponent(actionComponent, index, comState, componentStoreState)
                else -> null
            }
        }

        patcher.after<Message>("shouldShowReplyPreviewAsAttachment") { param ->
            if (this.isComponentV2) param.result = true
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        unpatchGson()
        ComponentV2Type.unmake(logger)
    }

    private fun patchGson() {
        val factory = ComponentRuntimeTypeAdapter.INSTANCE.a()
        val typeToClass = factory.l
        val classToType = factory.m
        ComponentV2Type.newValues?.forEach {
            typeToClass[it.type.toString()] = it.clazz
            classToType[it.clazz] = it.type.toString()
        }

        patcher.instead<ComponentTypeTypeAdapter>("read", JsonReader::class.java)
        { (_, jsonReader: JsonReader) ->
            val type: Int = b.c.a.a0.d.n1(jsonReader)
            ComponentType.values().find { it.type == type } ?: ComponentType.UNKNOWN
        }
    }
    private fun unpatchGson() {
        val factory = ComponentRuntimeTypeAdapter.INSTANCE.a()
        val typeToClass = factory.l
        val classToType = factory.m
        ComponentV2Type.newValues?.forEach {
            typeToClass.remove(it.type.toString())
            classToType.remove(it.clazz)
        }
    }
}
