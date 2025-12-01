package com.example.studybuddy.data;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;

public class CategoryPerformance {
    @ColumnInfo(name = "category")
    public String category;
    
    @ColumnInfo(name = "total")
    public int total;
    
    @ColumnInfo(name = "incorrect")
    public int incorrect;
    
    public CategoryPerformance() {
    }
    
    @Ignore
    public CategoryPerformance(String category, int total, int incorrect) {
        this.category = category;
        this.total = total;
        this.incorrect = incorrect;
    }
}

