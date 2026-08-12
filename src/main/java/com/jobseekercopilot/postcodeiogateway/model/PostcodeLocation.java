package com.jobseekercopilot.postcodeiogateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostcodeLocation {
    private String postcode;
    private String country;
    private String region;
    @JsonProperty("parliamentary_constituency")
    private String parliamentaryConstituency;
    private String eastings;
    private String northings;
    private Double longitude;
    private Double latitude;
    @JsonProperty("european_electoral_region")
    private String europeanElectoralRegion;
    @JsonProperty("primary_care_trust")
    private String primaryCareTrust;
    @JsonProperty("nhs_ha")
    private String nhsHa;
    @JsonProperty("admin_county")
    private String adminCounty;
    @JsonProperty("admin_district")
    private String adminDistrict;
    @JsonProperty("admin_ward")
    private String adminWard;
}
