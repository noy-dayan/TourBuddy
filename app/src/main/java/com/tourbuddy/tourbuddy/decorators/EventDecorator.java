package com.tourbuddy.tourbuddy.decorators;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.List;

public class EventDecorator implements DayViewDecorator {
    private Drawable drawable;
    private HashSet<CalendarDay> dates;

    public EventDecorator(Drawable drawable, List<CalendarDay> calendarDays) {
        this.drawable = drawable;
        this.dates = new HashSet<>(calendarDays);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // Apply drawable to dayView
        view.setSelectionDrawable(drawable);
        // You can add more decorations as needed here
    }
}
