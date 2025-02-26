package domain.interfaces

import domain.classes.BookingOption

interface IDemoScraper {
    fun getBookingOptions(): List<BookingOption>
    fun bookTrip(from: String, to: String, optionTitle: String)
}