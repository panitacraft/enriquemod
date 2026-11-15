package com.panita.enriquecraft.profile.client;

/**
 * The day of the year a member was born, as the Panita API tells the server: the month and the day, never the year.
 *
 * @param month 1 to 12
 * @param day   1 to 31; the API does not check it against the month, so neither does this
 */
public record Birthday(int month, int day) {

    public Birthday {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Not a day of the year: month " + month + ", day " + day);
        }
    }
}
