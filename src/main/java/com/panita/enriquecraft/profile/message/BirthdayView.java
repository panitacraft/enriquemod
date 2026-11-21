package com.panita.enriquecraft.profile.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.profile.client.Birthday;

import java.util.List;

/** Writes a birthday the way players read dates: the day and the name of the month, without a year, which is never known. */
public final class BirthdayView {

    /** One template for each month, in order; each is a whole text of the messages file. */
    private static final List<String> MONTHS = List.of(ProfileMessages.Months.JANUARY, ProfileMessages.Months.FEBRUARY,
            ProfileMessages.Months.MARCH, ProfileMessages.Months.APRIL, ProfileMessages.Months.MAY, ProfileMessages.Months.JUNE,
            ProfileMessages.Months.JULY, ProfileMessages.Months.AUGUST, ProfileMessages.Months.SEPTEMBER,
            ProfileMessages.Months.OCTOBER, ProfileMessages.Months.NOVEMBER, ProfileMessages.Months.DECEMBER);

    private BirthdayView() {
    }

    /** The date as a message, such as "18 de agosto". */
    public static Message date(Birthday birthday) {
        return Message.plain(MONTHS.get(birthday.month() - 1)).with("day", birthday.day());
    }
}
