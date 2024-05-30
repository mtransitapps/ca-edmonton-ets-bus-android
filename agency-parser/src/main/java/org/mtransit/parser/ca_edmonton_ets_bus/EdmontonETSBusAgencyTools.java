package org.mtransit.parser.ca_edmonton_ets_bus;

import static org.mtransit.commons.Constants.EMPTY;
import static org.mtransit.commons.Constants.SPACE_;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://data.edmonton.ca/
public class EdmontonETSBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new EdmontonETSBusAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	public String getAgencyName() {
		return "ETS";
	}

	private static final String AGENCY_ID = "1"; // Edmonton Transit Service ONLY

	@Nullable
	@Override
	public String getAgencyId() {
		return AGENCY_ID;
	}

	@Override
	public boolean excludeAgency(@NotNull GAgency gAgency) {
		//noinspection deprecation
		if (gAgency.getAgencyId().equals("5")) { // Spruce Grove Transit // 1 route #560
			return KEEP;
		}
		return super.excludeAgency(gAgency);
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		//noinspection deprecation
		if (gRoute.getRouteId().equals("023R")) { // Classified as bus
			return EXCLUDE;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		final String tripHeadsignLC = gTrip.getTripHeadsignOrDefault().toLowerCase(Locale.ENGLISH);
		if (tripHeadsignLC.contains("not in service")) {
			return EXCLUDE;
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@NotNull
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		//noinspection deprecation
		return gRoute.getRouteId(); // route ID string as route short name used by real-time API
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true; // used by GTFS-RT
	}

	@Nullable
	@Override
	public Long convertRouteIdFromShortNameNotSupported(@NotNull String routeShortName) {
		switch (routeShortName) {
		case "ValRep":
			return 9_073L;
		case "Shuttle":
			return 999L;
		case "CapRep":
			return 9_071L;
		}
		return super.convertRouteIdFromShortNameNotSupported(routeShortName);
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String gRouteLongName) {
		gRouteLongName = CleanUtils.cleanStreetTypes(gRouteLongName);
		return CleanUtils.cleanLabel(gRouteLongName);
	}

	@Override
	public boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge) {
		if (mRoute.simpleMergeLongName(mRouteToMerge)) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		//noinspection ConstantConditions
		if (true) { // isGoodEnoughAccepted()
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		throw new MTLog.Fatal("Unexpected routes to merge: %s & %s!", mRoute, mRouteToMerge);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	@NotNull
	@Override
	public List<Integer> getDirectionTypes() {
		return Collections.singletonList(
				// MTrip.HEADSIGN_TYPE_DIRECTION // <= mixed w/ string not supported (yet?)
				MTrip.HEADSIGN_TYPE_STRING
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern BAY_AZ09 = Pattern.compile("( bay [a-z0-9]+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_X_ = Pattern.compile("(^X )", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_DASH_OWL_ = Pattern.compile("(^-OWL )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = cleanTripHeadsign(fromStopName, directionHeadSign);
		directionHeadSign = BAY_AZ09.matcher(directionHeadSign).replaceAll(SPACE_);
		directionHeadSign = STARTS_WITH_DASH_OWL_.matcher(directionHeadSign).replaceAll(EMPTY);
		return directionHeadSign;
	}

	private static final String S_ = "S ";

	@Nullable
	@Override
	public String selectDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2) {
		if (StringUtils.equals(headSign1, headSign2)) {
			return null; // canNOT select
		}
		if (headSign1 != null && headSign1.startsWith(S_)) {
			if (headSign2 == null || !headSign2.startsWith(S_)) {
				return headSign2;
			}
		} else if (headSign2 != null && headSign2.startsWith(S_)) {
			return headSign1;
		}
		return null;
	}

	private static final Pattern SUPER_EXPRESS = CleanUtils.cleanWords("super express");

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^\\d+( )?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		return cleanTripHeadsign(false, tripHeadsign);
	}

	@NotNull
	private String cleanTripHeadsign(boolean fromStopName, @NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		if (!fromStopName) {
			tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(EMPTY);
			tripHeadsign = STARTS_WITH_X_.matcher(tripHeadsign).replaceAll(EMPTY);
		}
		tripHeadsign = TOWN_CENTER.matcher(tripHeadsign).replaceAll(TOWN_CENTER_REPLACEMENT);
		tripHeadsign = SUPER_EXPRESS.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = INTERNATIONAL.matcher(tripHeadsign).replaceAll(INTERNATIONAL_REPLACEMENT);
		tripHeadsign = BELVEDERE_.matcher(tripHeadsign).replaceAll(BELVEDERE_REPLACEMENT);
		tripHeadsign = EDMONTON.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final String TOWN_CENTER_SHORT = "TC";
	private static final Pattern TOWN_CENTER = CleanUtils.cleanWords("town center", "town centre");
	private static final String TOWN_CENTER_REPLACEMENT = CleanUtils.cleanWordsReplacement(TOWN_CENTER_SHORT);

	private static final Pattern INTERNATIONAL = CleanUtils.cleanWords("international");
	private static final String INTERNATIONAL_REPLACEMENT = CleanUtils.cleanWordsReplacement("Int");

	private static final String BELVEDERE = "Belvedere";
	private static final Pattern BELVEDERE_ = CleanUtils.cleanWords("belevedere");
	private static final String BELVEDERE_REPLACEMENT = CleanUtils.cleanWordsReplacement(BELVEDERE);

	private static final String EDMONTON_SHORT = "Edm";
	private static final Pattern EDMONTON = CleanUtils.cleanWords("edmonton");
	private static final String EDMONTON_REPLACEMENT = CleanUtils.cleanWordsReplacement(EDMONTON_SHORT);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = TOWN_CENTER.matcher(gStopName).replaceAll(TOWN_CENTER_REPLACEMENT);
		gStopName = INTERNATIONAL.matcher(gStopName).replaceAll(INTERNATIONAL_REPLACEMENT);
		gStopName = BELVEDERE_.matcher(gStopName).replaceAll(BELVEDERE_REPLACEMENT);
		gStopName = EDMONTON.matcher(gStopName).replaceAll(EDMONTON_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopIdS = gStop.getStopId();
		if (!CharUtils.isDigitsOnly(stopIdS)) {
			if (Character.isAlphabetic(stopIdS.charAt(0))) {
				final int stopIdInt2 = Integer.parseInt(stopIdS.substring(1));
				return getStopIdForLetter(stopIdS.substring(0, 1)) * 10_000 + stopIdInt2;
			}
		}
		return Math.abs(Integer.parseInt(stopIdS)); // remove negative stop IDs
	}

	private int getStopIdForLetter(String letter) {
		switch (letter.toUpperCase(Locale.ROOT)) {
		// @formatter:off
		case "A": return 1;
		case "P": return 16;
		case "S": return 19;
		// @formatter:on
		default:
			throw new MTLog.Fatal("Unexpected letter '%s'!");
		}
	}

	private static final Pattern REMOVE_STARTING_DASH = Pattern.compile("(^-)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		String stopCode = super.getStopCode(gStop); // do not change, used by real-time API & GTFS-RT
		stopCode = REMOVE_STARTING_DASH.matcher(stopCode).replaceAll(EMPTY);
		if (!CharUtils.isDigitsOnly(stopCode)) {
			if (!Character.isAlphabetic(stopCode.charAt(0))) {
				throw new MTLog.Fatal("Unexpected stop code %s!", gStop);
			}
		}
		return stopCode; // do not change, used by real-time API
	}
}
