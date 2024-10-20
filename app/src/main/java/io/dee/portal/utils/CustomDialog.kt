package io.dee.portal.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import io.dee.portal.R
import io.dee.portal.databinding.CustomDialogViewBinding

class CustomDialog private constructor(context: Context, builder: Builder) :
    Dialog(context, R.style.custom_dialog_theme) {

    private val icon: Int? = builder.getIcon()
    private val iconTint: Int? = builder.getIconTint()
    private val title: String = builder.getTitle()
    private val titleColor: Int? = builder.getTitleColor()
    private val description: String = builder.getDescription()
    private val descriptionColor: Int? = builder.getDescriptionColor()
    private val confirmActionString: String = builder.getConfirmActionString()
    private val cancelActionString: String = builder.getCancelActionString()
    private val confirmCallback: (dialog: CustomDialog) -> Unit = builder.getConfirmCallback()
    private val cancelCallback: (dialog: CustomDialog) -> Unit = builder.getCancelCallback()


    private lateinit var dialogBinding: CustomDialogViewBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialogBinding = CustomDialogViewBinding.inflate(LayoutInflater.from(context), null, false)
        dialogBinding.apply {
            tvTitle.text = title
            tvTitle.visibility = if (title.isEmpty()) View.GONE else View.VISIBLE
            titleColor?.let {
                tvTitle.setTextColor(ContextCompat.getColor(context, it))
            }

            tvDescription.text = description
            tvDescription.visibility = if (description.isEmpty()) View.GONE else View.VISIBLE
            descriptionColor?.let {
                tvDescription.setTextColor(ContextCompat.getColor(context, it))
            }

            tvConfirmButton.text = confirmActionString
            tvConfirmButton.visibility =
                if (confirmActionString.isEmpty()) View.GONE else View.VISIBLE

            tvDiscardButton.text = cancelActionString
            tvDiscardButton.visibility =
                if (cancelActionString.isEmpty()) View.GONE else View.VISIBLE

            if (icon != null) {
                ivIcon.visibility = View.VISIBLE
                ivIcon.setImageDrawable(ContextCompat.getDrawable(context, icon))
                iconTint?.let {
                    ivIcon.drawable?.setTint(ContextCompat.getColor(context, it))
                }
            } else {
                ivIcon.visibility = View.GONE
            }


            btnDiscard.setOnClickListener {
                cancelCallback.invoke(this@CustomDialog)
            }
            tvConfirmButton.setOnClickListener {
                confirmCallback.invoke(this@CustomDialog)
            }

        }
        this.setContentView(dialogBinding.root)
    }


    class Builder {
        @DrawableRes
        private var icon: Int? = null

        @ColorRes
        private var iconTint: Int? = null

        @ColorRes
        private var titleColor: Int? = null

        @ColorRes
        private var descriptionColor: Int? = null
        private var title: String = ""
        private var description: String = ""
        private var confirmActionString: String = "Confirm"
        private var cancelActionString: String = "Cancel"
        private var confirmCallback: (dialog: CustomDialog) -> Unit = {}
        private var cancelCallback: (dialog: CustomDialog) -> Unit = {}

        fun setIcon(@DrawableRes icon: Int, @ColorRes iconTint: Int? = null): Builder {
            this.icon = icon
            this.iconTint = iconTint
            return this
        }

        fun getIcon(): Int? = this.icon
        fun getIconTint(): Int? = this.iconTint
        fun getTitle(): String = this.title
        fun getTitleColor(): Int? = this.titleColor
        fun getDescriptionColor(): Int? = this.descriptionColor
        fun getDescription(): String = this.description
        fun getConfirmActionString(): String = this.confirmActionString
        fun getCancelActionString(): String = this.cancelActionString
        fun getConfirmCallback(): (dialog: CustomDialog) -> Unit = this.confirmCallback
        fun getCancelCallback(): (dialog: CustomDialog) -> Unit = this.cancelCallback

        fun setTitle(title: String, @ColorRes color: Int? = null): Builder {
            this.title = title
            this.titleColor = color
            return this
        }

        fun setDescription(description: String, @ColorRes color: Int? = null): Builder {
            this.description = description
            this.descriptionColor = color
            return this
        }

        fun setConfirmActionString(confirmActionString: String): Builder {
            this.confirmActionString = confirmActionString
            return this
        }

        fun setCancelActionString(cancelActionString: String): Builder {
            this.cancelActionString = cancelActionString
            return this
        }

        fun setConfirmCallback(confirmCallback: (dialog: CustomDialog) -> Unit): Builder {
            this.confirmCallback = confirmCallback
            return this
        }

        fun setCancelCallback(cancelCallback: (dialog: CustomDialog) -> Unit): Builder {
            this.cancelCallback = cancelCallback
            return this
        }

        fun build(context: Context): CustomDialog {
            return CustomDialog(context, this)
        }
    }
}
