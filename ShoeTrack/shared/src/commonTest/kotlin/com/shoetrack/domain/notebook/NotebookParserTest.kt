package com.shoetrack.domain.notebook

import com.shoetrack.domain.model.Money
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotebookParserTest {
    @Test
    fun readsACleanLine() {
        val entry =
            NotebookParser.parse(
                "03/10/26 Alba Calambas Maxten gris hombre T=140.000\nAbona 30.000 Saldo $110.000",
            )

        assertEquals("Alba Calambas", entry.customerName)
        assertEquals("Maxten gris hombre", entry.productDescription)
        assertEquals(Money.fromUnits(140_000), entry.totalPrice)
        assertEquals(Money.fromUnits(30_000), entry.paidAmount)
        assertEquals(LocalDate(2026, 10, 3), entry.lastPaymentDate)
    }

    @Test
    fun readsALineWhereTheTotalIsSubtractedFromThePayment() {
        val entry = NotebookParser.parse("12-10-26 Miguel Angel Maxten negra 43 T=100.000 abono 40.000 saldo 60.000")

        assertEquals(Money.fromUnits(100_000), entry.totalPrice)
        assertEquals(Money.fromUnits(40_000), entry.paidAmount)
        assertEquals(LocalDate(2026, 10, 12), entry.lastPaymentDate)
    }

    @Test
    fun readsTheRealPhotoTextOfAClosePhotoOfTheNotebook() {
        val text =
            """
            10
            2?-08-26 AleJondra Tefray de
            13509D40.QO
            1209-e cOnceiio 93. G00
            fbon
            03/101 26 Alba Cclambu s Maxlen grs
            honbe T= 140.000
            Abona 30-000 Saldo io.000
            """.trimIndent()

        val entry = NotebookParser.parse(text)

        // The last line that starts with a date is the layaway that is still open.
        assertEquals("Alba Cclambu", entry.customerName)
        assertEquals("Maxlen grs honbe", entry.productDescription)
        assertEquals(Money.fromUnits(140_000), entry.totalPrice)
        assertEquals(Money.fromUnits(30_000), entry.paidAmount)
        assertEquals(LocalDate(2026, 10, 3), entry.lastPaymentDate)
    }

    @Test
    fun readsTheRealPhotoTextWhenTheDateAndTheLastBalanceAreUnreadable() {
        val text =
            """
            ANDS
            RY
            04l 26 Allba Aleundiq Ryrz
            Maxten rs todu neg 1n
            Talla 40 Suldo 1so000
            Abon 5o.000 suldo
            LOO.O
            """.trimIndent()

        val entry = NotebookParser.parse(text)

        assertEquals("Allba Aleundiq", entry.customerName)
        assertEquals(Money.fromUnits(150_000), entry.totalPrice)
        assertEquals(Money.fromUnits(50_000), entry.paidAmount)
        assertNull(entry.lastPaymentDate)
    }

    @Test
    fun aSizeIsNotMistakenForThePrice() {
        val entry = NotebookParser.parse("25-08-26 Maryori Camayo Nike shox T:36 T=128.000 Abona 75.000 Saldo 53.000")

        assertEquals(Money.fromUnits(128_000), entry.totalPrice)
        assertEquals("Nike shox T:36", entry.productDescription)
    }

    @Test
    fun withoutATotalLabelThePriceIsTheBalanceBeforeTheFirstPayment() {
        val entry = NotebookParser.parse("14/10/26 Maira Otero Paris negro Saldo 90.000 Abona 20.000 Saldo 70.000")

        assertEquals(Money.fromUnits(90_000), entry.totalPrice)
        assertEquals(Money.fromUnits(20_000), entry.paidAmount)
    }

    @Test
    fun readsALayawayWithSeveralPaymentsWrittenOnSeparateDatedLines() {
        val text =
            """
            27-10-26 Paola Flor Vans blanco/negro alto T37
            80.000 Abona: $50.000 saldo: 30.000
            3-11-26 Abona $10.000 saldo: 20.000
            """.trimIndent()

        val entry = NotebookParser.parse(text)

        assertEquals("Paola Flor", entry.customerName)
        assertEquals("Vans blanco/negro alto T37", entry.productDescription)
        assertEquals(Money.fromUnits(80_000), entry.totalPrice)
        assertEquals(Money.fromUnits(60_000), entry.paidAmount)
        assertEquals(LocalDate(2026, 11, 3), entry.lastPaymentDate)
    }

    @Test
    fun readsTheRealPhotoTextWhereTheMonthWasReadAsLetters() {
        val text =
            """
            04 ll 26 Alba Alejundi Ryiz
            Maxten rs todu nneą tn
            Talla = 4b Suldo sbo00
            Abonu 5o 000 suldo
            (OO.0
            """.trimIndent()

        val entry = NotebookParser.parse(text)

        assertEquals("Alba Alejundi", entry.customerName)
        assertEquals(Money.fromUnits(50_000), entry.paidAmount)
        assertEquals(LocalDate(2026, 11, 4), entry.lastPaymentDate)
    }

    @Test
    fun anImpossibleDateIsLeftEmpty() {
        assertNull(NotebookParser.parse("45/13/26 Ana Perez Nike T=100.000").lastPaymentDate)
    }

    @Test
    fun textWithoutAnyLayawayGivesAnEmptyEntry() {
        assertEquals(NotebookEntry(), NotebookParser.parse("Sofia"))
        assertEquals(NotebookEntry(), NotebookParser.parse(""))
    }
}
