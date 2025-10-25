package org.example.auth.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Address {
    private String street_number;
    private String street_name;
    private String city;
    private String state;
    private String zip;

}
