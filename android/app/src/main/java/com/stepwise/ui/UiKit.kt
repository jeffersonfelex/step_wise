package com.stepwise.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.stepwise.R

/**
 * Kit de UI mínimo compartilhado entre as telas do StepWise. Todas as
 * telas são construídas em código Kotlin (sem XML de layout - ver
 * MainActivity/HardwareSessionActivity/SoftwareSessionActivity/
 * PrivacyActivity), então este arquivo existe só para dar consistência
 * visual (cores, tipografia, espaçamento, cantos arredondados) sem
 * duplicar os mesmos parâmetros em cada tela.
 *
 * Tudo aqui recebe [context] explicitamente (em vez de ser extension
 * function) para nunca depender de qual "this" implícito está em escopo
 * quando chamado de dentro de um bloco apply{} aninhado em outra Activity.
 */

fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

fun color(context: Context, colorRes: Int): Int = ContextCompat.getColor(context, colorRes)

/** Substitui completamente os LayoutParams de [view] por um LinearLayout.LayoutParams com as margens dadas. */
fun setLinearMargins(context: Context, view: android.view.View, top: Int = 0, bottom: Int = 0, start: Int = 0, end: Int = 0) {
    val params = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    params.setMargins(dp(context, start), dp(context, top), dp(context, end), dp(context, bottom))
    view.layoutParams = params
}

/** LayoutParams para um botão dentro de uma LinearLayout HORIZONTAL, dividindo o espaço igualmente. */
fun rowButtonParams(context: Context, weight: Float = 1f, marginEnd: Int = 8): LinearLayout.LayoutParams {
    val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
    params.setMargins(0, 0, dp(context, marginEnd), 0)
    return params
}

/** ScrollView com uma coluna vertical dentro, pronta para receber addView(). */
fun buildScrollableScreen(context: Context): Pair<ScrollView, LinearLayout> {
    val hPad = dp(context, 20)
    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(hPad, dp(context, 24), hPad, dp(context, 40))
    }
    val scroll = ScrollView(context).apply {
        isFillViewport = true
        setBackgroundColor(color(context, R.color.bg_base))
        addView(column)
    }
    return scroll to column
}

/** Barra superior: seta de voltar (opcional) + título + subtítulo opcional. */
fun buildTopBar(context: Context, title: CharSequence, subtitle: CharSequence? = null, onBack: (() -> Unit)? = null): LinearLayout {
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
    }
    setLinearMargins(context, row, bottom = 20)

    if (onBack != null) {
        val back = TextView(context).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(context, R.color.text_primary))
            setPadding(0, 0, dp(context, 16), 0)
            setOnClickListener { onBack() }
        }
        row.addView(back)
    }

    val titleColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }
    titleColumn.addView(TextView(context).apply {
        text = title
        textSize = 22f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(color(context, R.color.text_primary))
    })
    if (subtitle != null) {
        titleColumn.addView(TextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(color(context, R.color.text_secondary))
            setPadding(0, dp(context, 2), 0, 0)
        })
    }
    row.addView(titleColumn)
    return row
}

/** Rótulo pequeno em caixa alta, para separar seções dentro de uma tela. */
fun buildSectionLabel(context: Context, text: CharSequence): TextView = TextView(context).apply {
    this.text = text.toString().uppercase()
    textSize = 12f
    setTypeface(typeface, Typeface.BOLD)
    setTextColor(color(context, R.color.text_secondary))
    letterSpacing = 0.05f
    setLinearMargins(context, this, top = 20, bottom = 8)
}

/** Cartão de seção: MaterialCardView com uma coluna vertical dentro (com padding). */
fun buildCard(context: Context): Pair<MaterialCardView, LinearLayout> {
    val inner = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val pad = dp(context, 18)
        setPadding(pad, pad, pad, pad)
    }
    val card = MaterialCardView(context).apply {
        setRadius(dp(context, 20).toFloat())
        setCardElevation(0f)
        setCardBackgroundColor(color(context, R.color.bg_surface))
        setStrokeColor(color(context, R.color.outline))
        setStrokeWidth(dp(context, 1))
        addView(inner)
    }
    setLinearMargins(context, card, bottom = 14)
    return card to inner
}

fun buildBodyText(context: Context, text: CharSequence = ""): TextView = TextView(context).apply {
    this.text = text
    textSize = 15f
    setTextColor(color(context, R.color.text_primary))
    setLineSpacing(dp(context, 2).toFloat(), 1.1f)
}

fun buildCaptionText(context: Context, text: CharSequence = ""): TextView = TextView(context).apply {
    this.text = text
    textSize = 13f
    setTextColor(color(context, R.color.text_secondary))
}

/** Campo de texto simples com fundo arredondado (sem depender de TextInputLayout). */
fun buildTextInput(context: Context, hintText: CharSequence): EditText = EditText(context).apply {
    hint = hintText
    setTextColor(color(context, R.color.text_primary))
    setHintTextColor(color(context, R.color.text_hint))
    val pad = dp(context, 14)
    setPadding(pad, pad, pad, pad)
    background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, 14).toFloat()
        setColor(color(context, R.color.bg_surface_alt))
        setStroke(dp(context, 1), color(context, R.color.outline))
    }
}

private fun buildStyledButton(context: Context, text: CharSequence, styleRes: Int, onClick: () -> Unit): MaterialButton =
    MaterialButton(ContextThemeWrapper(context, styleRes)).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { onClick() }
    }

/** Botão primário (preenchido, cor de destaque do app). */
fun buildPrimaryButton(context: Context, text: CharSequence, onClick: () -> Unit): MaterialButton =
    buildStyledButton(context, text, R.style.Widget_StepWise_Button_Filled, onClick)

/** Botão secundário (contornado, mais discreto que o primário). */
fun buildSecondaryButton(context: Context, text: CharSequence, onClick: () -> Unit): MaterialButton =
    buildStyledButton(context, text, R.style.Widget_StepWise_Button_Outlined, onClick)

/** Botão destrutivo (contornado em vermelho - ex.: apagar histórico). */
fun buildDestructiveButton(context: Context, text: CharSequence, onClick: () -> Unit): MaterialButton =
    buildStyledButton(context, text, R.style.Widget_StepWise_Button_Destructive, onClick)

/** Botão contornado em verde (ex.: forçar resultado "correto" na demo). */
fun buildSuccessButton(context: Context, text: CharSequence, onClick: () -> Unit): MaterialButton =
    buildStyledButton(context, text, R.style.Widget_StepWise_Button_Success, onClick)

/** Botão contornado em âmbar (ex.: forçar "erro conhecido" na demo). */
fun buildWarningButton(context: Context, text: CharSequence, onClick: () -> Unit): MaterialButton =
    buildStyledButton(context, text, R.style.Widget_StepWise_Button_Warning, onClick)

/** "Pill" colorida (status/etiqueta curta) - fundo translúcido tintado programaticamente. */
fun buildStatusPill(context: Context, text: CharSequence, colorRes: Int): TextView = TextView(context).apply {
    this.text = text
    textSize = 12f
    setTypeface(typeface, Typeface.BOLD)
    setTextColor(color(context, colorRes))
    val padH = dp(context, 10)
    val padV = dp(context, 5)
    setPadding(padH, padV, padH, padV)
    val base = color(context, colorRes)
    background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, 20).toFloat()
        setColor(
            android.graphics.Color.argb(
                38,
                android.graphics.Color.red(base),
                android.graphics.Color.green(base),
                android.graphics.Color.blue(base)
            )
        )
    }
}
