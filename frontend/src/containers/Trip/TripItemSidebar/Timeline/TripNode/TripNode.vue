<template>
  <div v-bind:class="hasPermissionToEdit ? 'trip-destination-cursor' : '' ">
    <v-timeline-item  
      :style="{width: '90%', marginLeft: '5%'}"
      color="primary"
      small
      :right="alignRight"
      :hide-dot="tripNode.nodeType === 'TripComposite'"
    >
      <v-card v-bind:class="(tripNode.arrivalDate && tripNode.departureDate) ? '' : 'no-date'">
        <v-card-title class="secondary trip-destination-title">
          <v-text-field
            v-if="isEditingTrip"
            autofocus
            v-model="editedTripName"
            @blur="nameEdited"
            @keyup.enter="nameEdited"
            color="white"
            class="trip-name-field"
            :rules="tripNameRules"
          ></v-text-field>
          <h3
            v-else
            @click="goToTripNode()"
            class="white--text font-weight-light"
          >{{ tripNode.name }}</h3>

          <v-spacer align="right" v-if="hasPermissionToEdit">
            <v-btn class="edit-btn" flat @click="editTripNode">
              <v-icon v-if="!isEditingTrip">edit</v-icon>
            </v-btn>

            <v-btn class="delete-btn" flat @click="$emit('deleteTripNode', tripNode)" v-if="hasPermissionToUnlink">
              <v-icon v-if="isTripDestinationLeaf">delete</v-icon>
              <v-icon v-else>link_off</v-icon>
            </v-btn>
          </v-spacer>
        </v-card-title>
        <div class="container">
          <v-layout>
            <v-flex xs2>
              <v-icon size="30">flight_takeoff</v-icon>
            </v-flex>
            <v-flex xs10 class="date-info">
              <p>{{ tripNode.arrivalTotal }}</p>
            </v-flex>
          </v-layout>
          <v-layout>
            <v-flex xs2>
              <v-icon size="30">flight_landing</v-icon>
            </v-flex>
            <v-flex xs10 class="date-info">
              <p>{{ tripNode.departureTotal }}</p>
            </v-flex>
          </v-layout>
          <v-spacer align="center" v-if="tripNode.nodeType === 'TripComposite'">
            <v-btn
              @click="toggleShowTripNodes(tripNode)"
              tile
              outlined
              color="secondary"
            >
              <v-icon left>{{ tripNode.isShowing ? "keyboard_arrow_up" : "keyboard_arrow_down"}}</v-icon>
              {{ tripNode.isShowing ? 'Collapse' : 'Expand' }}
            </v-btn>
          </v-spacer>
        </div>
      </v-card>

      <div
        v-if="tripNode.nodeType === 'TripComposite'"
        v-bind:class="{ expanded: tripNode.nodeType === 'TripComposite'
    && tripNode.isShowing}"
        id="trip-nodes"
      >
        <div>
          <Timeline
            @deleteTripNode="tripNode => $emit('deleteTripNode', tripNode)"
            @toggleExpanded="tripNode => $emit('toggleExpanded', tripNode)"
            @showEditTripDestination="tripNode => $emit('showEditTripDestination', tripNode)"
            :trip="tripNode"
            isSubTrip
            :rootTrip="rootTrip"
          />
        </div>
      </div>
    </v-timeline-item>
  </div>
</template>


<script>
import moment from "moment";
import { rules } from "../../../../../utils/rules";
import UserStore from "../../../../../stores/UserStore";
import roleType from "../../../../../stores/roleType";

export default {
  components: {
    Timeline: () => import("../Timeline")
  },
  name: "TimelineDestination",
  props: {
    tripNode: Object,
    alignRight: Boolean,
    rootTrip: Object,
    parentTrip: Object,
  },
  data() {
    return {
      isEditingTrip: false,
      editedTripName: "",
      tripNameRules: [rules.required]
    };
  },
  computed: {
    /**
     * Checks if the trip node is a TripDestinationLeaf, and returns true if it is.
     */
    isTripDestinationLeaf() {
      return this.tripNode.nodeType === "TripDestinationLeaf";
    },
    /**
     * Checks if the user has the permission to edit the trip node
     */
    hasPermissionToEdit() {
      let userRole;
      if (this.tripNode.nodeType === "TripDestinationLeaf") {
        userRole = this.parentTrip.userRoles.find(
          userRole => userRole.user.userId === UserStore.data.userId
        );
      } else {
        userRole = this.tripNode.userRoles.find(
          userRole => userRole.user.userId === UserStore.data.userId
        );
      }

      const isTripManager = userRole && userRole.role.roleType === roleType.TRIP_MANAGER;
      const isTripOwner = userRole && userRole.role.roleType === roleType.TRIP_OWNER;

      return isTripManager || isTripOwner;
    },
    /**
     * Checks if the user is a trip manager or owner, if they are, this returns true. Otherwise
     * returns false.
     */
    hasPermissionToUnlink() {
      const userRole = this.parentTrip.userRoles.find(
        userRole => userRole.user.userId === UserStore.data.userId
      );

      const isTripManager = userRole && userRole.role.roleType === roleType.TRIP_MANAGER;
      const isTripOwner = userRole && userRole.role.roleType === roleType.TRIP_OWNER;

      return isTripManager || isTripOwner;
    }
  },
  methods: {
    /**
     * Emits the toggle expanded trip node component
     */
    toggleShowTripNodes(tripNode) {
      this.$emit("toggleExpanded", tripNode.tripNodeId);
    },
    /**
     * If the trip name is edited, it emits the function that indicates the trip name is updated.
     */
    nameEdited() {
      if (this.editedTripName && this.editedTripName.length > 0) {
        if (this.editedTripName !== this.tripNode.name) {
          this.isEditingTrip = false;
          this.$emit("tripNameUpdated", this.tripNode, this.editedTripName);
        } else {
          this.isEditingTrip = false;
        }
      }
    },
    /**
     * Either goes to sub-trip if trip node is a trip, or destination
     */
    goToTripNode() {
      if (this.tripNode.nodeType === "TripComposite") {
        this.$router.push(`/trips/${this.tripNode.tripNodeId}`);
      } else {
        this.$router.push(
          `/destinations/${this.tripNode.destination.destinationId}`
        );
      }
    },
    /**
     * Checks if the trip node is a trip composite, and sets the trip name to the trip node name and the editing
     * trip into true.
     */
    editTripNode() {
      if (this.tripNode.nodeType === "TripComposite") {
        this.editedTripName = this.tripNode.name;
        this.isEditingTrip = true;
      } else {
        this.$emit("showEditTripDestination", this.tripNode);
      }
    }
  }
};
</script>

<style lang="scss" scoped>
.trip-destination-title {
  padding: 5px;
}

.container {
  padding: 5px;
}

.trip-destination-cursor {
  cursor: move;
}

.date-info {
  padding-left: 10px;
  padding-top: 8px;

  p {
    // Override default p style to make text align from the left
    text-align: left;
  }
}

.edit-btn,
.delete-btn {
  margin: 0;
  min-width: 0px;
  width: 10px;
  color: #fff;
  font-size: 0.4rem;
}

.expand-trip {
  font-size: 2.5rem;
  cursor: pointer;
}

.no-date {
  background-color: #dddddd;
}

.light-text {
  color: #fff;
}
</style>

<style lang="scss">
.v-timeline--dense .v-timeline-item__body {
  max-width: calc(100% - 22px) !important;
}

#trip-nodes {
  max-height: 0px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.expanded {
  // You can't transition a height to auto so need to specify
  // an arbitrarily large max height
  max-height: 300rem !important;
}

h3 {
  cursor: pointer;
}

.trip-name-field {
  -webkit-text-fill-color: white;
}
</style>


