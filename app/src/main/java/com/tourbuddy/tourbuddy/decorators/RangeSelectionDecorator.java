package com.tourbuddy.tourbuddy.decorators;

import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import org.threeten.bp.LocalDate;

import java.util.List;

public class RangeSelectionDecorator implements DayViewDecorator {

    private final Drawable startDrawable;
    private final Drawable middleDrawable;
    private final Drawable endDrawable;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate currentDate;

    public RangeSelectionDecorator(Drawable startDrawable, Drawable middleDrawable, Drawable endDrawable) {
        this.startDrawable = startDrawable;
        this.middleDrawable = middleDrawable;
        this.endDrawable = endDrawable;
    }

    public void setSelectedDates(List<CalendarDay> dates) {
        if(dates != null) {
            if (dates.size() >= 2) {
                this.startDate = dates.get(0).getDate();
                this.endDate = dates.get(dates.size() - 1).getDate();

            }
            else if (dates.size() == 1){
                this.startDate = dates.get(0).getDate();
                this.endDate = startDate;

            }
        }
    }


    @Override
    public boolean shouldDecorate(CalendarDay day) {
        this.currentDate = day.getDate();
        return startDate != null && endDate != null &&
                !currentDate.isBefore(startDate) && !currentDate.isAfter(endDate);
    }

    @Override
    public void decorate(@NonNull DayViewFacade view) {
        if (currentDate != null && startDate != null && endDate != null) {
            if (currentDate.equals(startDate)) {
                view.setSelectionDrawable(startDrawable);
            } else if (currentDate.equals(endDate)) {
                view.setSelectionDrawable(endDrawable);
            } else {
                view.setSelectionDrawable(middleDrawable);
            }
        }
    }

    public void clearSelection() {
        startDate = null;
        endDate = null;
    }
}
