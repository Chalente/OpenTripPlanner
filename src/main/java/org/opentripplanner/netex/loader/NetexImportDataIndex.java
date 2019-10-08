package org.opentripplanner.netex.loader;

import org.opentripplanner.netex.loader.util.HierarchicalElement;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.opentripplanner.netex.loader.util.HierarchicalVersionMapById;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalVersionMapById;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TimetabledPassingTime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class holds indexes of Netex objects for lookup during the NeTEx import using the
 * {@link NetexImportDataIndexReadOnlyView}.
 * <p>
 * A NeTEx import is grouped into several levels: <em>shard data</em>, <em>group of shared data</em>,
 * and <em>single files</em>. We create a hierarchy of {@code NetexImportDataIndex} to avoid keeping everything
 * in memory and to be able to override values in a more specific(lower) level.
 * <p/>
 * There is one instance of this class for <em>shard data</em> - the ROOT.
 * For each <em>group of shared data</em> a new {@code NetexImportDataIndex} is created with the ROOT as a
 * parent. When such <em>group of shared data</em> is not needed any more it is discard and become
 * ready for garbage collection.
 * For each <em>single files</em> a new {@code NetexImportDataIndex} is created with the corresponding
 * <em>group of shared data</em> as parent. The <em>single files</em> object is thrown away when
 * the file is loaded.
 * <p/>
 * This hierarchy make it possible to override values in child instances of the {@code NetexImportDataIndex}
 * and save memory during the load operation, because data not needed any more can be thrown away.
 * <p/>
 * The hierarchy implementation is delegated to the
 * {@link org.opentripplanner.netex.loader.util.AbstractHierarchicalMap} and the
 * {@link HierarchicalElement} classes.
 * <p/>
 * The mapping code should not insert entities, so an instance of this class implements the
 * {@link NetexImportDataIndexReadOnlyView} witch is passed to the mapping code for translation into
 * OTP domain model objects.
 */
public class NetexImportDataIndex {

    // Indexes to entities
    public final HierarchicalMapById<Authority> authoritiesById;
    public final HierarchicalMapById<DayType> dayTypeById;
    public final HierarchicalMultimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId;
    /**
     * DayTypeRefs is only needed in the local scope, no need to lookup values in the parent.
     * */
    public final Set<DayTypeRefsToServiceIdAdapter> dayTypeRefs;
    public final HierarchicalMapById<DestinationDisplay> destinationDisplayById;
    public final HierarchicalMapById<GroupOfLines> groupOfLinesById;
    public final HierarchicalMapById<JourneyPattern> journeyPatternsById;
    public final HierarchicalMapById<Line> lineById;
    public final HierarchicalMapById<Network> networkById;
    public final HierarchicalMapById<Notice> noticeById;
    public final HierarchicalMapById<NoticeAssignment> noticeAssignmentById;
    public final HierarchicalMapById<OperatingPeriod> operatingPeriodById;
    public final HierarchicalMultimap<String, TimetabledPassingTime> passingTimeByStopPointId;
    public final HierarchicalVersionMapById<Quay> quayById;
    public final HierarchicalMap<String, String> quayIdByStopPointRef;
    public final HierarchicalMapById<Route> routeById;
    public final HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId;
    public final HierarchicalVersionMapById<StopPlace> stopPlaceById;


    // Relations between entities - The Netex XML sometimes rely on the the
    // nested structure of the XML document, rater than explicit references.
    // Since we throw away the document we need to keep track of these.

    public final HierarchicalMap<String, String> networkIdByGroupOfLineId;


    // Shared data
    public final HierarchicalElement<String> timeZone;


    /**
     * Create a root node.
     */
    public NetexImportDataIndex() {
        this.authoritiesById = new HierarchicalMapById<>();
        this.dayTypeById = new HierarchicalMapById<>();
        this.dayTypeAssignmentByDayTypeId = new HierarchicalMultimap<>();
        this.dayTypeRefs = new HashSet<>();
        this.destinationDisplayById = new HierarchicalMapById<>();
        this.groupOfLinesById = new HierarchicalMapById<>();
        this.journeyPatternsById = new HierarchicalMapById<>();
        this.lineById = new HierarchicalMapById<>();
        this.networkById = new HierarchicalMapById<>();
        this.networkIdByGroupOfLineId = new HierarchicalMap<>();
        this.noticeById = new HierarchicalMapById<>();
        this.noticeAssignmentById = new HierarchicalMapById<>();
        this.operatingPeriodById = new HierarchicalMapById<>();
        this.passingTimeByStopPointId = new HierarchicalMultimap<>();
        this.quayById = new HierarchicalVersionMapById<>();
        this.quayIdByStopPointRef = new HierarchicalMap<>();
        this.routeById = new HierarchicalMapById<>();
        this.serviceJourneyByPatternId = new HierarchicalMultimap<>();
        this.stopPlaceById = new HierarchicalVersionMapById<>();
        this.timeZone = new HierarchicalElement<>();
    }

    /**
     * Create a child node.
     * @param parent can not be <code>null</code>.
     */
    NetexImportDataIndex(NetexImportDataIndex parent) {
        this.authoritiesById = new HierarchicalMapById<>(parent.authoritiesById);
        this.dayTypeById = new HierarchicalMapById<>(parent.dayTypeById);
        this.dayTypeAssignmentByDayTypeId = new HierarchicalMultimap<>(parent.dayTypeAssignmentByDayTypeId);
        this.dayTypeRefs = new HashSet<>();
        this.destinationDisplayById = new HierarchicalMapById<>(parent.destinationDisplayById);
        this.groupOfLinesById = new HierarchicalMapById<>(parent.groupOfLinesById);
        this.journeyPatternsById = new HierarchicalMapById<>(parent.journeyPatternsById);
        this.lineById = new HierarchicalMapById<>(parent.lineById);
        this.networkById = new HierarchicalMapById<>(parent.networkById);
        this.networkIdByGroupOfLineId = new HierarchicalMap<>(parent.networkIdByGroupOfLineId);
        this.noticeById = new HierarchicalMapById<>(parent.noticeById);
        this.noticeAssignmentById = new HierarchicalMapById<>(parent.noticeAssignmentById);
        this.operatingPeriodById = new HierarchicalMapById<>(parent.operatingPeriodById);
        this.passingTimeByStopPointId = new HierarchicalMultimap<>(parent.passingTimeByStopPointId);
        this.quayById = new HierarchicalVersionMapById<>(parent.quayById);
        this.quayIdByStopPointRef = new HierarchicalMap<>(parent.quayIdByStopPointRef);
        this.routeById = new HierarchicalMapById<>(parent.routeById);
        this.serviceJourneyByPatternId = new HierarchicalMultimap<>(parent.serviceJourneyByPatternId);
        this.stopPlaceById = new HierarchicalVersionMapById<>(parent.stopPlaceById);
        this.timeZone = new HierarchicalElement<>(parent.timeZone);
    }

    public NetexImportDataIndexReadOnlyView readOnlyView() {
        return new NetexImportDataIndexReadOnlyView() {

            /**
             * Lookup a Network given a GroupOfLine id or an Network id. If the given
             * {@code groupOfLineOrNetworkId} is a GroupOfLine ID, we lookup the GroupOfLine, and then
             * lookup its Network. If the given {@code groupOfLineOrNetworkId} is a Network ID then we
             * can lookup the Network directly.
             * <p/>
             * If no Network is found {@code null} is returned.
             */
            public Network lookupNetworkForLine(String groupOfLineOrNetworkId) {
                GroupOfLines groupOfLines = groupOfLinesById.lookup(groupOfLineOrNetworkId);

                String networkId = groupOfLines == null
                        ? groupOfLineOrNetworkId
                        : networkIdByGroupOfLineId.lookup(groupOfLines.getId());

                return networkById.lookup(networkId);
            }

            public ReadOnlyHierarchicalMapById<Authority> getAuthoritiesById() {
                return authoritiesById;
            }

            public ReadOnlyHierarchicalMapById<DayType> getDayTypeById() {
                return dayTypeById;
            }

            public ReadOnlyHierarchicalMap<String, Collection<DayTypeAssignment>> getDayTypeAssignmentByDayTypeId() {
                return dayTypeAssignmentByDayTypeId;
            }

            public Iterable<DayTypeRefsToServiceIdAdapter> getDayTypeRefs() {
                return Collections.unmodifiableSet(dayTypeRefs);
            }

            public ReadOnlyHierarchicalMapById<DestinationDisplay> getDestinationDisplayById() {
                return destinationDisplayById;
            }

            public ReadOnlyHierarchicalMapById<JourneyPattern> getJourneyPatternsById() {
                return journeyPatternsById;
            }

            public ReadOnlyHierarchicalMapById<Line> getLineById() {
                return lineById;
            }

            public ReadOnlyHierarchicalMapById<Notice> getNoticeById() {
                return noticeById;
            }

            public ReadOnlyHierarchicalMapById<NoticeAssignment> getNoticeAssignmentById() {
                return noticeAssignmentById;
            }

            public ReadOnlyHierarchicalMapById<OperatingPeriod> getOperatingPeriodById() {
                return operatingPeriodById;
            }

            public ReadOnlyHierarchicalMap<String, Collection<TimetabledPassingTime>> getPassingTimeByStopPointId() {
                return passingTimeByStopPointId;
            }

            public ReadOnlyHierarchicalVersionMapById<Quay> getQuayById() {
                return quayById;
            }

            public ReadOnlyHierarchicalMap<String, String> getQuayIdByStopPointRef() {
                return quayIdByStopPointRef;
            }

            public ReadOnlyHierarchicalMapById<Route> getRouteById() {
                return routeById;
            }

            public ReadOnlyHierarchicalMap<String, Collection<ServiceJourney>> getServiceJourneyByPatternId() {
                return serviceJourneyByPatternId;
            }

            public ReadOnlyHierarchicalVersionMapById<StopPlace> getStopPlaceById() {
                return stopPlaceById;
            }

            public String getTimeZone() {
                return timeZone.get();
            }
        };
    }
}