package com.sek789102.formulabook.domain.study

import com.sek789102.formulabook.ui.viewmodel.StudyFormulaItem
import kotlin.random.Random

data class MatchCardEntry(
    val cardId: String,
    val pairId: String,
    val text: String,
    val subjectId: String,
    val isFlashcard: Boolean
)

data class MatchRoundState(
    val leftCards: List<MatchCardEntry>,
    val rightCards: List<MatchCardEntry>,
    val matchedPairIds: Set<String> = emptySet(),
    val selectedLeftId: String? = null,
    val selectedRightId: String? = null,
    val mismatchPair: Pair<String, String>? = null,
    val attempts: Int = 0,
    val mistakes: Int = 0
) {
    val totalPairs: Int get() = leftCards.size
    val isCompleted: Boolean get() = totalPairs > 0 && matchedPairIds.size >= totalPairs
    val accuracyPercent: Int
        get() = if (attempts <= 0) 100 else (((attempts - mistakes).coerceAtLeast(0) * 100f) / attempts).toInt()
}

object MatchGameEngine {

    fun buildRound(
        items: List<StudyFormulaItem>,
        maxPairs: Int = 5,
        random: Random = Random.Default
    ): MatchRoundState {
        val uniquePairs = items
            .mapNotNull { item ->
                val prompt = item.formula.title.ifBlank { item.noteTitle }.trim()
                val answer = item.formula.expression.trim()
                if (prompt.isBlank() || answer.isBlank()) null else Triple(item, prompt, answer)
            }
            .distinctBy { (_, prompt, answer) -> "$prompt||$answer" }
            .shuffled(random)
            .take(maxPairs.coerceAtLeast(2))

        if (uniquePairs.size < 2) {
            return MatchRoundState(leftCards = emptyList(), rightCards = emptyList())
        }

        val left = uniquePairs.mapIndexed { idx, (item, prompt, _) ->
            val pairId = "${item.noteId}_${item.formula.id}_$idx"
            MatchCardEntry(
                cardId = "L_$pairId",
                pairId = pairId,
                text = prompt,
                subjectId = item.subjectId,
                isFlashcard = item.formula.isFlashcard
            )
        }

        var right = uniquePairs.mapIndexed { idx, (item, _, answer) ->
            val pairId = "${item.noteId}_${item.formula.id}_$idx"
            MatchCardEntry(
                cardId = "R_$pairId",
                pairId = pairId,
                text = answer,
                subjectId = item.subjectId,
                isFlashcard = item.formula.isFlashcard
            )
        }.shuffled(random)

        if (right.size >= 2 && right.map { it.pairId } == left.map { it.pairId }) {
            right = right.drop(1) + right.first()
        }

        return MatchRoundState(
            leftCards = left,
            rightCards = right
        )
    }

    fun selectLeft(state: MatchRoundState, cardId: String): MatchRoundState {
        val card = state.leftCards.firstOrNull { it.cardId == cardId } ?: return state
        if (card.pairId in state.matchedPairIds || state.isCompleted) return state

        val cleared = state.copy(mismatchPair = null)
        if (cleared.selectedLeftId == cardId) {
            return cleared.copy(selectedLeftId = null)
        }

        val rightId = cleared.selectedRightId
        if (rightId == null) {
            return cleared.copy(selectedLeftId = cardId)
        }
        return resolvePair(cleared.copy(selectedLeftId = cardId), cardId, rightId)
    }

    fun selectRight(state: MatchRoundState, cardId: String): MatchRoundState {
        val card = state.rightCards.firstOrNull { it.cardId == cardId } ?: return state
        if (card.pairId in state.matchedPairIds || state.isCompleted) return state

        val cleared = state.copy(mismatchPair = null)
        if (cleared.selectedRightId == cardId) {
            return cleared.copy(selectedRightId = null)
        }

        val leftId = cleared.selectedLeftId
        if (leftId == null) {
            return cleared.copy(selectedRightId = cardId)
        }
        return resolvePair(cleared.copy(selectedRightId = cardId), leftId, cardId)
    }

    fun clearMismatch(state: MatchRoundState): MatchRoundState {
        return if (state.mismatchPair != null) state.copy(mismatchPair = null) else state
    }

    private fun resolvePair(state: MatchRoundState, leftId: String, rightId: String): MatchRoundState {
        val left = state.leftCards.firstOrNull { it.cardId == leftId } ?: return state
        val right = state.rightCards.firstOrNull { it.cardId == rightId } ?: return state

        return if (left.pairId == right.pairId) {
            state.copy(
                matchedPairIds = state.matchedPairIds + left.pairId,
                selectedLeftId = null,
                selectedRightId = null,
                mismatchPair = null,
                attempts = state.attempts + 1
            )
        } else {
            state.copy(
                selectedLeftId = null,
                selectedRightId = null,
                mismatchPair = leftId to rightId,
                attempts = state.attempts + 1,
                mistakes = state.mistakes + 1
            )
        }
    }
}
