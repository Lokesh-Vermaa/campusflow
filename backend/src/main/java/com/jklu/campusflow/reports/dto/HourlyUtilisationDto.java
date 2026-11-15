package com.jklu.campusflow.reports.dto;

import java.util.List;

public record HourlyUtilisationDto(
        String hour,
        double occupancyRate
) {}
