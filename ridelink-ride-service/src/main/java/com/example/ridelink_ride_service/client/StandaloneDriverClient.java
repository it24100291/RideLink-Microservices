package com.example.ridelink_ride_service.client;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Component
@Profile("standalone")
public class StandaloneDriverClient implements DriverGateway {
    private final Map<String,Reservation> reservations=new HashMap<>();
    private String active;
    public synchronized Reservation reserve(String id) {
        var previous=reservations.get(id);
        if(previous!=null) {
            if(previous.released()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Reservation already released");
            return previous;
        }
        if(active!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Demo driver is busy");
        var result=new Reservation(id,1L,2L,false);
        reservations.put(id,result); active=id; return result;
    }
    public synchronized void release(String id) {
        reservations.put(id,new Reservation(id,1L,2L,true));
        if(id.equals(active)) active=null;
    }
}
