package util;

import java.time.LocalDate;
import java.util.Objects;

public class SimpleDate {

    private int year;
    private int month;
    private int day;

    public SimpleDate(){
        LocalDate localDate = LocalDate.now();
        year = localDate.getYear();
        month = localDate.getMonthValue();
        day = localDate.getDayOfMonth();
        
    }

    public SimpleDate(int year, int month, int day){
        this.year = year;
        this.month = month;
        this.day = day;
    }


    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDate that = (SimpleDate) o;
        return year == that.year &&
                month == that.month &&
                day == that.day;
    }

    public boolean sameMonthDay(int month, int day){
        return month == this.month && day == this.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }

    @Override
    public String toString() {
        return year+"-"+month+"-"+day;
    }
}
