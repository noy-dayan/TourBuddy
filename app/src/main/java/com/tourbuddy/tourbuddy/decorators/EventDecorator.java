package com.tourbuddy.tourbuddy.decorators;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;


import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.List;

public class EventDecorator implements DayViewDecorator {
    private Context context;
    private Drawable drawable;
    private HashSet<CalendarDay> dates;

    public EventDecorator(Context context, int drawableResId, List<CalendarDay> calendarDays) {
        this.context = context;
        this.drawable = ContextCompat.getDrawable(context, drawableResId);
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
        // White text color
        view.addSpan(new ForegroundColorSpan(ContextCompat.getColor(context, android.R.color.white)));
    }
}
