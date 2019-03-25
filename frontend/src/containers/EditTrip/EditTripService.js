import superagent from "superagent";
import { endpoint } from "../../utils/endpoint";
import moment from "moment";

function formatTime(time) {
  return moment.utc(time.as("milliseconds")).format("HH:mm");
}

/**
 * 
 * @param {Object} trip The trip to transform
 * @param {string} trip.tripName The name of the trip
 * @param {Object[]} trip.tripDestinations The destinations in a trip
 * @param {number} trip.tripDestinations[].arrivalDate The arrival date of the destination
 * @param {number} trip.tripDestinations[].arrivalTime The arrival time of the destination
 * @param {number} trip.tripDestinations[].departureDate The departure date of the destination
 * @param {number} trip.tripDestinations[].departureTime The departure time of the destination
 * @return {Object} The transformed trip
 */ 
export function transformTripResponse(trip) {
  return {
    tripName: trip.tripName,
    tripDestinations: trip.tripDestinations.map(tripDestination => {
      return {
        destinationId: tripDestination.destination.destinationId,
        arrivalDate: tripDestination.arrivalDate === 0 ? null : moment(tripDestination.arrivalDate).format("YYYY-MM-DD"),
        arrivalTime: tripDestination.arrivalTime === -1 ? null : formatTime(moment.duration(tripDestination.arrivalTime, "minutes")),
        departureDate: tripDestination.departureDate === 0 ? null : moment(tripDestination.departureDate).format("YYYY-MM-DD"),
        departureTime: tripDestination.departureTime === -1 ? null : formatTime(moment.duration(tripDestination.departureTime, "minutes")),
      };
    }),
  };
}

/**
 * Sends a request to get a trip
 * @param {number} tripId The trip ID to get
 */
export async function getTrip(tripId) {
  const userId = localStorage.getItem("userId");
  const authToken = localStorage.getItem("authToken");
  const res = await superagent.get(endpoint(`/travellers/${userId}/trips/${tripId}`))
    .set("Authorization", authToken);
  return res.body;
}

/**
 * 
 * @param {number} tripId - The ID of the trip to edit
 * @param {string} tripName - The edited trip name
 * @param {Object[]} tripDestinations - The edited trip destinations
 */
export function editTrip(tripId, tripName, tripDestinations) {

   const transformedTripDestinations = tripDestinations.map(tripDestination => {
    const transformedTripDestination = {};

    transformedTripDestination.destinationId = tripDestination.destinationId;
    transformedTripDestination.arrivalDate = moment(tripDestination.arrivalDate).valueOf();
    transformedTripDestination.arrivalTime = tripDestination.arrivalTime === null ? null : moment.duration(tripDestination.arrivalTime).asMinutes();
    transformedTripDestination.departureDate = moment(tripDestination.departureDate).valueOf(); 
    transformedTripDestination.departureTime = tripDestination.departureTime === null ? null : moment.duration(tripDestination.departureTime).asMinutes();

    return transformedTripDestination;
  }); 


  const userId = localStorage.getItem("userId");
  const authToken = localStorage.getItem("authToken");

  return superagent.put(endpoint(`/travellers/${userId}/trips/${tripId}`))
    .set("Authorization", authToken)
    .send({
      tripName,
      tripDestinations: transformedTripDestinations
    });
}
