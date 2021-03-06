import superagent from "superagent";
import { endpoint } from "../../utils/endpoint";
import moment from "moment";

/**
 * Sends a request to add a trip.
 * @param {string} name name of the trip to add.
 * @param {Array<Object>} tripNodes list of trip destinations to add as part of the trip.
 * @param {Array<number>} userIds The userID's to add to the trip
 * @return response from backend.
 */
export async function createTrip(name, tripNodes, userIds) {
  const userId = localStorage.getItem("userId");
  const transformedTripNodes = transformTripNodes(tripNodes);
  const res = await superagent.post(endpoint(`/users/${userId}/trips`))
  .set("Authorization", localStorage.getItem("authToken"))
  .send({
    name,
    tripNodes: transformedTripNodes,
    userIds
  });
  return res.body;
}

/**
 * Transforms the trip nodes to the proper trip node format
 * @param tripNodes the trip nodes to be formatted
 * @returns the transformed formatted trip nodes
 */
function transformTripNodes(tripNodes) {
  return tripNodes.map(tripNode => {
    let transformedTripNode = {};
    transformedTripNode.nodeType = "TripDestinationLeaf";
    transformedTripNode.destinationId = tripNode.destinationId;
    transformedTripNode.arrivalDate = moment(tripNode.arrivalDate).valueOf();
    transformedTripNode.arrivalTime =
        tripNode.arrivalTime === null ? null : moment.duration(tripNode.arrivalTime).asMinutes();
    transformedTripNode.departureDate = moment(tripNode.departureDate).valueOf();
    transformedTripNode.departureTime =
        tripNode.departureTime === null ? null : moment.duration(tripNode.departureTime).asMinutes();
    return transformedTripNode;
  });
}

/**
 * Gets all users that match the given search parameter
 * @param name or partial name to search for
 * @returns {Array} Returns a list of users
 */
export async function getUsers(name) {
  const authToken = localStorage.getItem("authToken");
  const res = await superagent.get(endpoint(`/users/search`)).query({name: name})
    .set("Authorization", authToken);

  return res.body;
}
