package com.davidsalama.lumiform.domain.model

data class ResponseSet(
    val id: Long,
    val multipleSelection: Boolean,
    val responses: List<Response>,
) {
    /**
     * Returns the selection after the user taps [tappedId].
     *
     * - Multiple selection: tapping adds the response, tapping it again removes it.
     * - Single selection: tapping replaces the selection, tapping the selected one clears it,
     *   so a wrong tap can always be undone back to "no answer".
     *
     * Ids that aren't part of this set leave the selection unchanged.
     */
    fun toggle(current: Set<Long>, tappedId: Long): Set<Long> {
        if (responses.none { it.id == tappedId }) return current
        return when {
            tappedId in current -> current - tappedId
            multipleSelection -> current + tappedId
            else -> setOf(tappedId)
        }
    }
}

data class Response(
    val id: Long,
    val label: String,
    val score: Int?,
)
