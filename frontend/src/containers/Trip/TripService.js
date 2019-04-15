import superagent from "superagent";
import { endpoint } from "../../utils/endpoint";
import moment from "moment";

/**
 * Sends a request to get a trip
 * @param {number} userId 
 * @param {number} tripId 
 */
export async function getTrip(userId, tripId) {
  const res = await superagent.get(endpoint(`/users/${userId}/trips/${tripId}`))
    .set("Authorization", localStorage.getItem("authToken"));
  return res.body;
}

/**
 * Formats a time duration as a string
 * @param {number} time 
 * @return {string} formatted time
 */
function formatTime(time) {
  return moment.utc(time.as('milliseconds')).format('HH:mm');
}

/**
 * Formats arrival and departure date time
 * @param {number} date 
 * @param {number} time 
 */
function formatDateTime(date, time) {
  if (date === 0 && time === -1) {
    return "";
  } else if (date && time !== -1) {
    return `${moment(date).format("DD/YY/YYYY")} at ${formatTime(moment.duration(time, "minutes"))}`
  } else if (date) {
    return moment(date).format("DD/MM/YYYY")
  } else {
    return formatTime(moment.duration(time, "minutes"))
  }
}

/**
 * Transforms trip response into readable formats
 * @param {Object} trip 
 * @param {string} trip.tripName
 * @param {Object[]} trip.tripDestinations
 * @return {Object} transformed trip
 * 
 */
export function transformTrip(trip) {
  return {
    tripName: trip.tripName,
      tripDestinations: trip.tripDestinations.map(tripDestination => {
        return {
          arrival: formatDateTime(tripDestination.arrivalDate, tripDestination.arrivalTime),
          departure: formatDateTime(tripDestination.departureDate, tripDestination.departureTime),
          destinationName: tripDestination.destination.destinationName
        };
    })
  } 
}