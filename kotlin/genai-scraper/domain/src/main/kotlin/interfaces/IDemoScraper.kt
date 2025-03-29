package interfaces

import classes.data.BookingOption

interface IDemoScraper {
    fun getBookingOptions(): List<BookingOption>
}