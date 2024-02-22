package com.tourbuddy.tourbuddy.decorators;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.List;

/**
 * Decorator class to highlight event days on the calendar.
 */
public class EventDecorator implements DayViewDecorator {
    Drawable drawable;
    HashSet<CalendarDay> dates;

    /**
     * Constructor for EventDecorator.
     *
     * @param drawable     The drawable to be used for highlighting the event days.
     * @param calendarDays List of CalendarDay objects representing event days.
     */
    public EventDecorator(Drawable drawable, List<CalendarDay> calendarDays) {
        this.drawable = drawable;
        this.dates = new HashSet<>(calendarDays);
    }

    /**
     * Determines if a day should be decorated.
     *
     * @param day The day to check for decoration.
     * @return True if the day should be decorated, false otherwise.
     */
    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    /**
     * Apply decoration to the given day view.
     *
     * @param view The facade for the day view to be decorated.
     */
    @Override
    public void decorate(DayViewFacade view) {
        // Apply drawable to dayView
        view.setSelectionDrawable(drawable);
        // You can add more decorations as needed here
    }
}
