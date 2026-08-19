package com.utown.utownbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rider_profiles")
public class RiderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "transport_type", nullable = false)
    private String transportType;

    @Column(nullable = false)
    private Boolean availability;

    @Column(nullable = false)
    private String status;

    public RiderProfile() {
    }

    public RiderProfile(
            User user,
            String transportType,
            Boolean availability,
            String status
    ) {
        this.user = user;
        this.transportType = transportType;
        this.availability = availability;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public Boolean getAvailability() {
        return availability;
    }

    public void setAvailability(Boolean availability) {
        this.availability = availability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}