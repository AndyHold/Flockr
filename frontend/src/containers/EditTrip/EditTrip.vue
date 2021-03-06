<template>
	<div>
		<v-card
						id="edit-trip"
						class="col-lg-10 offset-lg-1"
		>

			<h2>Edit Trip</h2>

			<v-form ref="editTripForm">
				<v-text-field
								v-model="tripName"
								label="Trip Name"
								color="secondary"
								class="col-md-6"
								:rules="tripNameRules"
				>
				</v-text-field>

				<TripTable :tripDestinations="tripDestinations" :isEditing="true" @showErrorSnackbar="showErrorSnackbar"/>

				<v-btn
								depressed
								color="secondary"
								small
								id="add-destination"
								@click="addDestination"
				>
					<v-icon>add</v-icon>
				</v-btn>

				<v-btn
								depressed
								color="secondary"
								id="edit-trip-btn"
								@click="editTrip()"
				>Save
				</v-btn>
			</v-form>
		</v-card>

		<Snackbar
			:snackbarModel="snackbarModel"
			></Snackbar>
	</div>
</template>

<script>
  import TripTable from "../../components/TripTable/TripTable";
  import {getTrip, transformTripResponse} from "./EditTripService.js";
  import {editTrip} from "../Trip/TripService";

  const rules = {
    required: field => !!field || "Field required"
  };

  // empty tripDestinations to add when the user presses "+"
  const tripDestination = {
    destinationId: null,
    arrivalDate: null,
    arrivalTime: null,
    departureDate: null,
    departureTime: null,
  };

  export default {
    components: {
      TripTable
    },
    data() {
      return {
        tripName: "",
        tripDestinations: [],
        tripNameRules: [rules.required],
        travellerId: 0
      };
    },
    mounted() {
      this.getTrip();
      this.travellerId = this.$route.params.travellerId;
    },
    methods: {
			/**
			 * @param {String} message the message to show in the snackbar
			 * @param {String} color the colour for the snackbar
			 * @param {Number} the amount of time (in ms) for which we show the snackbar
			 */
			showSnackbar(message, color, timeout) {
				this.$root.$emit("show-snackbar", {
					message: message,
					color: color,
					timeout: timeout
				});
			},
      /**
			 * Displays a snackbar with an error message.
			 */
      showErrorSnackbar(message) {
				this.showSnackbar(message, "error", 3000);
			},
      /**
       * Gets a users trip for editing
       */
      async getTrip() {
        try {
          const tripId = this.$route.params.id;

          const travellerId = this.$route.params.travellerId;
          const userId = travellerId ? travellerId : localStorage.getItem("userId");
          const trip = await getTrip(tripId, userId);

          const {tripName, tripDestinations} = transformTripResponse(trip);

          this.tripName = tripName;
          this.tripDestinations = tripDestinations;
        } catch (e) {
          this.showErrorSnackbar("Could not get user trip");
        }
      },
      /**
       * Adds an empty destination
       */
      addDestination() {
        // ... spread operator clones object instead of referencing
        this.tripDestinations.push({...tripDestination, id: this.tripDestinations.length});
      },
      /**
       * Iterates through destinations and check and renders error message
       * if destinations are contiguous
       */
      contiguousDestinations() {
        let foundContiguousDestination = false;
        for (let i = 1; i < this.tripDestinations.length; i++) {

          if (this.tripDestinations[i].destinationId === this.tripDestinations[i - 1].destinationId && this.tripDestinations[i].destinationId) {
            this.$set(this.tripDestinations[i], "destinationErrors", ["Destination is same as last destination"]);
            foundContiguousDestination = true;
            continue;
          }
          this.tripDestinations[i].destinationErrors = [];
        }
        return foundContiguousDestination;
      },
      /**
       * Validates fields before sending a request to add a trip
       */
      async editTrip() {
        const validFields = this.$refs.editTripForm.validate();
        const contiguousDestinations = this.contiguousDestinations();
        if (!validFields || contiguousDestinations) {
          return;
        }

        try {
          const tripId = this.$route.params.id;
          await editTrip(tripId, this.tripName, this.tripDestinations);

          if (this.travellerId) {
            this.$router.push(`/travellers/${this.travellerId}/trips/${tripId}`);
          } else {
            this.$router.push(`/trips/${tripId}`)
          }

        } catch (e) {
          this.showErrorSnackbar("Error editing trip");
        }
      }
    }
  };
</script>

<style lang="scss" scoped>
  #edit-trip {
    align-self: center;
    margin-top: 30px;
    padding: 20px;

    h2 {
      text-align: center;
    }
  }

  #add-destination {
    margin-top: 10px !important;
    display: block;
    margin: 0 auto;
  }

  #edit-trip-btn {
    float: right;
  }
</style>


