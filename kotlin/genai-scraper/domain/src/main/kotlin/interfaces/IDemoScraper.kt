package interfaces

import classes.data.BookingOption

interface IDemoScraper {
    fun getBookingOptions(): List<BookingOption>
    fun bookTrip(from: String, to: String, optionTitle: String)
}