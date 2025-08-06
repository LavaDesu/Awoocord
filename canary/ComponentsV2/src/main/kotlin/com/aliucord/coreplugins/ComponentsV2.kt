package com.aliucord.coreplugins

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.*
import com.aliucord.coreplugins.componentsv2.patchMessageItems
import com.aliucord.coreplugins.componentsv2.views.*
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.discord.api.botuikit.*
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
import com.lytefast.flexinput.R
import de.robv.android.xposed.XposedBridge

val Message.isComponentV2 get() = (flags shr 15) and 1 == 1L

@AliucordPlugin(requiresRestart = true)
@Suppress("unused")
class ComponentsV2 : Plugin() {
    override fun start(context: Context) {
        compat(patcher)
        XposedBridge.makeClassInheritable(BotUiComponentEntry::class.java)
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
        stopCompat()
    }
}
