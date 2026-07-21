package com.jobseekercopilot.postcodeiogateway.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceLocation {
    private String id;
    private String name;
    private String postcode;
    private String region;
    private String adminDistrict;
    private Double latitude;
    private Double longitude;
}
