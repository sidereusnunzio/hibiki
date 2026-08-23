package com.hibiki.ui.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroSwipeToRandomTest {
    @Test
    fun atTop_downwardSwipePastThreshold_triggers() {
        assertTrue(
            shouldTriggerHeroSwipeToRandom(
                atTop = true,
                downwardDistancePx = 80f,
                minDistancePx = 40f,
            ),
        )
    }

    @Test
    fun scrolledAwayFromTop_doesNotTrigger() {
        assertFalse(
            shouldTriggerHeroSwipeToRandom(
                atTop = false,
                downwardDistancePx = 120f,
                minDistancePx = 40f,
            ),
        )
    }

    @Test
    fun atTop_tinyMovement_doesNotTrigger() {
        assertFalse(
            shouldTriggerHeroSwipeToRandom(
                atTop = true,
                downwardDistancePx = 12f,
                minDistancePx = 40f,
            ),
        )
    }

    @Test
    fun visualOffset_clampsNegativeToZero() {
        assertEquals(0f, heroSwipeToRandomVisualOffset(-20f, maxOffsetPx = 80f), 0.01f)
    }

    @Test
    fun visualOffset_followsWithinMax() {
        assertEquals(40f, heroSwipeToRandomVisualOffset(40f, maxOffsetPx = 80f), 0.01f)
    }

    @Test
    fun visualOffset_rubberBandsPastMax() {
        val offset = heroSwipeToRandomVisualOffset(120f, maxOffsetPx = 80f)
        assertTrue(offset > 80f)
        assertTrue(offset < 120f)
    }

    @Test
    fun labelAlpha_scalesWithPullProgress() {
        assertEquals(0f, heroSwipeRandomLabelAlpha(0f, minDistancePx = 40f), 0.01f)
        assertEquals(0.5f, heroSwipeRandomLabelAlpha(20f, minDistancePx = 40f), 0.01f)
        assertEquals(1f, heroSwipeRandomLabelAlpha(40f, minDistancePx = 40f), 0.01f)
        assertEquals(1f, heroSwipeRandomLabelAlpha(80f, minDistancePx = 40f), 0.01f)
    }

    @Test
    fun postScroll_atTop_consumesDownwardDelta() {
        assertEquals(24f, heroSwipeConsumePostScrollY(atTop = true, availableY = 24f), 0.01f)
    }

    @Test
    fun postScroll_notAtTop_orUpward_consumesNothing() {
        assertEquals(0f, heroSwipeConsumePostScrollY(atTop = false, availableY = 24f), 0.01f)
        assertEquals(0f, heroSwipeConsumePostScrollY(atTop = true, availableY = -10f), 0.01f)
    }

    @Test
    fun preScroll_reducesActivePullWithoutGoingNegative() {
        assertEquals(-12f, heroSwipeConsumePreScrollY(currentRawPullPx = 20f, availableY = -12f), 0.01f)
        assertEquals(-20f, heroSwipeConsumePreScrollY(currentRawPullPx = 20f, availableY = -40f), 0.01f)
        assertEquals(0f, heroSwipeConsumePreScrollY(currentRawPullPx = 0f, availableY = -12f), 0.01f)
        assertEquals(0f, heroSwipeConsumePreScrollY(currentRawPullPx = 20f, availableY = 8f), 0.01f)
    }
}
