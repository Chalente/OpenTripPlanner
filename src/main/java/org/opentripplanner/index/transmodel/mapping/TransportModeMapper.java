package org.opentripplanner.index.transmodel.mapping;

import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.TraverseMode;

public class TransportModeMapper {


    // Co
    public static TraverseMode getTraverseMode(int routeType) {
        /* TPEG Extension  https://groups.google.com/d/msg/gtfs-changes/keT5rTPS7Y0/71uMz2l6ke0J */
        if (routeType >= 100 && routeType < 200){ // Railway Service
            return TraverseMode.RAIL;
        }else if (routeType >= 200 && routeType < 300){ //Coach Service
            return TraverseMode.BUS;
        }else if (routeType >= 300 && routeType < 500){ //Suburban Railway Service and Urban Railway service
            if (routeType >= 401 && routeType <= 402) {
                return TraverseMode.SUBWAY;
            }
            return TraverseMode.RAIL;
        }else if (routeType >= 500 && routeType < 700){ //Metro Service and Underground Service
            return TraverseMode.SUBWAY;
        }else if (routeType >= 700 && routeType < 900){ //Bus Service and Trolleybus service
            return TraverseMode.BUS;
        }else if (routeType >= 900 && routeType < 1000){ //Tram service
            return TraverseMode.TRAM;
        }else if (routeType >= 1000 && routeType < 1100){ //Water Transport Service
            return TraverseMode.FERRY;
        }else if (routeType >= 1100 && routeType < 1200){ //Air Service
            return TraverseMode.AIRPLANE;
        }else if (routeType >= 1200 && routeType < 1300){ //Ferry Service
            return TraverseMode.FERRY;
        }else if (routeType >= 1300 && routeType < 1400){ //Telecabin Service
            return TraverseMode.GONDOLA;
        }else if (routeType >= 1400 && routeType < 1500){ //Funicalar Service
            return TraverseMode.FUNICULAR;
        }else if (routeType >= 1500 && routeType < 1600){ //Taxi Service
            throw new IllegalArgumentException("Taxi service not supported" + routeType);
        }else if (routeType >= 1600 && routeType < 1700){ //Self drive
            return TraverseMode.CAR;
        }
        /* Original GTFS route types. Should these be checked before TPEG types? */
        switch (routeType) {
            case 0:
                return TraverseMode.TRAM;
            case 1:
                return TraverseMode.SUBWAY;
            case 2:
                return TraverseMode.RAIL;
            case 3:
                return TraverseMode.BUS;
            case 4:
                return TraverseMode.FERRY;
            case 5:
                return TraverseMode.CABLE_CAR;
            case 6:
                return TraverseMode.GONDOLA;
            case 7:
                return TraverseMode.FUNICULAR;
            default:
                throw new IllegalArgumentException("unknown gtfs route type " + routeType);
        }
    }
}
