package hei.school.kofiatrans.service;

import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.model.Trip;

import java.time.LocalDate;
import java.util.List;

public class RevenueService {
    private static final double COOPERATIVE_RATE = 0.15;

    private final DataRetriever dataRetriever;

    public RevenueService(DataRetriever dataRetriever) {
        this.dataRetriever = dataRetriever;
    }

    public long computeDriverRevenue(String driverId, LocalDate from, LocalDate to) {
        return dataRetriever.findTripsByDriver(driverId).stream()
                .filter(Trip::isBillable)
                .filter(trip -> !trip.getTripDate().isBefore(from) && !trip.getTripDate().isAfter(to))
                .mapToLong(Trip::getBilledAmount)
                .sum();
    }

    public long computeCooperativeFee(String driverId, LocalDate from, LocalDate to) {
        long revenue = computeDriverRevenue(driverId, from, to);
        return Math.round(revenue * COOPERATIVE_RATE);
    }

    public double computeAveragePricePerKm(String driverId) {
        List<Trip> completedTrips = dataRetriever.findTripsByDriver(driverId).stream()
                .filter(Trip::isBillable)
                .toList();
        if (completedTrips.isEmpty()) {
            return 0.0;
        }
        return completedTrips.stream()
                .mapToDouble(Trip::pricePerKm)
                .average()
                .orElse(0.0);
    }

    public int computeCompletedTripsCount(String driverId) {
        return (int) dataRetriever.findTripsByDriver(driverId).stream()
                .filter(Trip::isBillable)
                .count();
    }

    public long computeTotalDistanceKm(String driverId) {
        return dataRetriever.findTripsByDriver(driverId).stream()
                .filter(Trip::isBillable)
                .mapToLong(Trip::getDistanceKm)
                .sum();
    }

    public Driver findTopEarningDriver(LocalDate from, LocalDate to) {
        Driver topDriver = null;
        long topRevenue = -1;
        for (Driver driver : dataRetriever.findAllDrivers()) {
            long revenue = computeDriverRevenue(driver.getId(), from, to);
            if (revenue > topRevenue) {
                topRevenue = revenue;
                topDriver = driver;
            }
        }
        return topDriver;
    }
}
