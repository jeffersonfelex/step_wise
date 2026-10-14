package com.stepwise.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stepwise.R
import com.stepwise.data.db.AppDatabase
import com.stepwise.data.db.SessionStateEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela de privacidade (LGPD) — ver claude/lgpd-privacidade.md.
 *
 * O aprendiz precisa poder ver, a qualquer momento, o que foi registrado
 * sobre o próprio desempenho (etapa, nível, resultado — nunca frames ou
 * áudio, que não são persistidos) e apagar esse histórico quando quiser,
 * sem depender de suporte técnico.
 */
class PrivacyActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var emptyLabel: TextView
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val hPad = dp(ctx, 20)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(hPad, dp(ctx, 24), hPad, dp(ctx, 20))
            setBackgroundColor(color(ctx, R.color.bg_base))
        }

        root.addView(buildTopBar(ctx, getString(R.string.privacy_title), onBack = { finish() }))

        val (explanationCard, explanationInner) = buildCard(ctx)
        explanationInner.addView(buildBodyText(ctx, getString(R.string.privacy_explanation)))
        root.addView(explanationCard)

        emptyLabel = buildCaptionText(ctx, getString(R.string.privacy_empty))
        historyContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val (historyCard, historyInner) = buildCard(ctx)
        historyInner.addView(emptyLabel)
        historyInner.addView(historyContainer)

        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(historyCard)
        }
        root.addView(scroll)

        val deleteButton = buildDestructiveButton(ctx, getString(R.string.privacy_delete_all)) { confirmDelete() }
        deleteButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        setLinearMargins(ctx, deleteButton, top = 14)
        root.addView(deleteButton)

        setContentView(root)
        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(applicationContext).sessionStateDao()
            val entries = dao.listAll()
            renderHistory(entries)
        }
    }

    private fun renderHistory(entries: List<SessionStateEntity>) {
        val ctx = this
        historyContainer.removeAllViews()
        emptyLabel.visibility = if (entries.isEmpty()) TextView.VISIBLE else TextView.GONE
        for (entry in entries) {
            val row = buildBodyText(
                ctx,
                getString(
                    R.string.privacy_entry_format,
                    dateFormat.format(Date(entry.timestampMillis)),
                    entry.procedureId,
                    entry.stepId,
                    entry.assistanceLevel,
                    entry.lastResult
                )
            )
            row.textSize = 13f
            setLinearMargins(ctx, row, top = 6, bottom = 6)
            historyContainer.addView(row)
        }
    }

    private fun confirmDelete() {
        // Diálogo nativo de confirmação (não é um alerta de página web) —
        // exigido antes de uma ação destrutiva e irreversível (LGPD:
        // apagar é definitivo, sem lixeira/undo).
        AlertDialog.Builder(this)
            .setTitle(R.string.privacy_delete_confirm_title)
            .setMessage(R.string.privacy_delete_confirm_message)
            .setPositiveButton(R.string.privacy_delete_confirm_positive) { _, _ -> deleteAll() }
            .setNegativeButton(R.string.privacy_delete_confirm_negative, null)
            .show()
    }

    private fun deleteAll() {
        lifecycleScope.launch {
            AppDatabase.getInstance(applicationContext).sessionStateDao().clearAll()
            loadHistory()
        }
    }
}
