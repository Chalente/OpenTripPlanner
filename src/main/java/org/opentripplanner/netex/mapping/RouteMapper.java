package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    TransportModeMapper transportModeMapper = new TransportModeMapper();
    AgencyMapper agencyMapper = new AgencyMapper();

    public org.opentripplanner.model.Route mapRoute(Line line, OtpTransitBuilder transitBuilder, NetexDao netexDao, String timeZone){

        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();
        Network network = netexDao.lookupNetworkByLineId(line.getId());
        GroupOfLines groupOfLines = netexDao.lookupGroupOfLinesByLineId(line.getId());

        Agency agency;

        if (network != null && netexDao.lookupAuthorityByNetworkId(network.getId()) != null) {
            Authority authority = netexDao.lookupAuthorityByNetworkId(network.getId());
            String agencyId = authority.getId();
            agency = transitBuilder.getAgencies().stream().filter(a -> a.getId().equals(agencyId)).findFirst().get();
            otpRoute.setAgency(agency);
        } else if (groupOfLines != null && netexDao.lookupAuthorityByGroupOfLinesId(groupOfLines.getId()) != null) {
            Authority authority = netexDao.lookupAuthorityByGroupOfLinesId(groupOfLines.getId());
            String agencyId = authority.getId();
            agency = transitBuilder.getAgencies().stream().filter(a -> a.getId().equals(agencyId)).findFirst().get();
            otpRoute.setAgency(agency);
        } else {
            LOG.warn("No authority found for " + line.getId());
            agency = agencyMapper.getDefaultAgency(timeZone);
            String agencyId = agency.getId();
            if (!transitBuilder.getAgencies().stream().anyMatch(a -> a.getId().equals(agencyId))) {
                transitBuilder.getAgencies().add(agency);
            }
        }

        otpRoute.setAgency(agency);

        otpRoute.setId(AgencyAndIdFactory.createAgencyAndId(line.getId()));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        otpRoute.setType(transportModeMapper.getTransportMode(line.getTransportMode(), line.getTransportSubmode()));

        // Temp fix
        if (otpRoute.getShortName() == null)
            otpRoute.setShortName("");

        return otpRoute;
    }
}