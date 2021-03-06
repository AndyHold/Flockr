<template>
  <div id="destinations">
    <div id="map">
      <DestinationMap
        :destinations="getDestinationsCurrentlyViewing()"
        @coordinates-selected="addCoordinates"
      />
    </div>

    <DestinationSidebar
      :viewOption="viewOption"
      :yourDestinations="yourDestinations"
      :publicDestinations="publicDestinations"
      v-on:viewOptionChanged="viewOptionChanged"
      v-on:addDestinationClicked="addDestinationClicked"
      @addNewDestination="addNewDestination"
      @refreshDestinations="refreshDestinations"
      @search-criterion-updated="searchCriterionUpdated"
      @get-more-public-destinations="getMorePublicDestinations"
      :destinationsLoading="destinationsLoading"
      ref="sidebar"
      :latitude="latitude"
      :longitude="longitude"
    />
  </div>
</template>

<script>
  import DestinationSidebar from "./DestinationSidebar/DestinationSidebar";
  import DestinationMap from "../../components/DestinationMap/DestinationMap";
  import {
    getPublicDestinations,
    getYourDestinations,
    sendDeleteDestination,
    sendUndoDeleteDestination
  } from "./DestinationsService";
  import Command from "../../components/UndoRedo/Command";

  export default {
    components: {
      DestinationSidebar,
      DestinationMap
    },
    data() {
      return {
        destination: null,
        latitude: null,
        longitude: null,
        yourDestinations: [],
        publicDestinations: [],
        showCreateDestDialog: false,
        viewOption: "your",
        searchCriterion: '', // to ask API to only return destinations with this in the name
        destinationsLoading: false // whether there is a pending API call to get destinations
      };
    },
    mounted() {
      this.getYourDestinations();
      this.getPublicDestinations();
    },
    methods: {
      /**
       * Called to get more public destinations by a lazy loading implementation
       */
      async getMorePublicDestinations() {
        const numberOfPublicDestinations = this.publicDestinations.length;
        this.destinationsLoading = true;
        try {
          const newDestinations = await getPublicDestinations(this.searchCriterion, numberOfPublicDestinations);
          this.publicDestinations = [...this.publicDestinations, ...newDestinations];
          this.destinationsLoading = false;
        } catch (err) {
          this.$root.$emit('show-error-snackbar', 'Could not get more public destinations', 3000);
          this.destinationsLoading = false;
        }
      },
      /**
       * Called when the search criterion is updated
       */
      searchCriterionUpdated(newValue) {
        this.searchCriterion = newValue;
        this.getPublicDestinations();
      },
      /**
       * Sets the latitude and longitude coordinates to the given coordinates
       */
      addCoordinates(coordinates) {
        const { latitude, longitude } = coordinates;
        this.latitude = latitude;
        this.longitude = longitude;
      },
      /**
       * Refreshes the sidebar when the button is toggled from your destinations and public destinations and vice versa.
       */
      refreshDestinations() {
        if (this.viewOption === "your") {
          this.getYourDestinations();
        } else {
          this.getPublicDestinations();
        }
      },
      /**
       * Gets destinations for the logged in user
       */
      async getYourDestinations() {
        try {
          this.destinationsLoading = true;
          this.yourDestinations = await getYourDestinations();
          this.destinationsLoading = false;
        } catch (e) {
          this.showSnackbar("Could not get your destinations", "error", 3000);
          this.destinationsLoading = false;
        }
      },
      /**
       * Gets all public destinations
       */
      async getPublicDestinations() {
        try {
          const { searchCriterion } = this;
          this.destinationsLoading = true;
          this.publicDestinations  = await getPublicDestinations(searchCriterion, 0);
          this.destinationsLoading = false;
        } catch (e) {
          this.showSnackbar("Could not get public destinations", "error", 3000);
          this.destinationsLoading = false;
        }
      },
      /**
       * Emit event from sidebar indicating that the user has swapped the type of
       * destinations to view
       */
      viewOptionChanged(viewOption) {
        this.viewOption = viewOption;
        // If user wants to load public destinations and they haven't been loaded, then load
        if (viewOption === "public" && !this.publicDestinations) {
          this.getPublicDestinations();
        }
      },
      /**
       * @param {String} message the message to show in the snackbar
       * @param {String} color the colour for the snackbar
       * @param {Number} timeout the amount of time (in ms) for which we show the snackbar
       */
      showSnackbar(message, color, timeout) {
        this.$root.$emit("show-snackbar", {
          message: message,
          color: color,
          timeout: timeout
        });
      },
      /**
       * Shows create destination dialog
       */
      addDestinationClicked() {
        this.resetCoordinates();
        this.showCreateDestDialog = true;
      },
      /**
       * Add a new destination to the system. Push the undo/redo commands to the stack.
       * @param destination new destination.
       */
      addNewDestination(destination) {
        this.yourDestinations.push(destination);
        this.getYourDestinations();
        this.getPublicDestinations();

        const undoCommand = async (destination) => {
          try {
            await sendDeleteDestination(destination.destinationId);
            this.yourDestinations.splice(this.yourDestinations.indexOf(destination));
            this.getYourDestinations();
            this.getPublicDestinations();
          } catch (error) {
            this.showSnackbar("Could not undo the action", "error", 3000);
          }
        };

        const redoCommand = async (destination) => {
          try {
            await sendUndoDeleteDestination(destination.destinationId);
            this.yourDestinations.push(destination);
            this.getYourDestinations();
            this.getPublicDestinations();
            [].shift()
          } catch (error) {
            this.showSnackbar("Could not redo the action", "error", 3000);
          }
        };

        const updateDestCommand = new Command(undoCommand.bind(null, destination), redoCommand.bind(null, destination));
        this.$refs.sidebar.addUndoRedoCommand(updateDestCommand);
        this.showCreateDestDialog = false;

      },
      /**
       * Show either your destinations or public destinations depending on view option.
       * @returns {*}
       */
      getDestinationsCurrentlyViewing() {
        const destinations = this.viewOption === "your" ? this.yourDestinations : this.publicDestinations;
        if (!destinations) return [];
        return destinations;
      }
    }
  }
</script>

<style lang="scss" scoped>
  #destinations {
    display: flex;
    width: 100%;
    height: 100%;

    #map {
      flex-grow: 1;
      height: 100%;
    }
  }
</style>




