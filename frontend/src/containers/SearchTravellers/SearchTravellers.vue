<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
  <div style="width: 100%">
    <div class="search-container">
      <!-- Min and Max Age Slider -->
      <!-- Nationality, Gender and Traveller Typ Select Input -->
      <v-card elevation="10" flat color="transparent" class="age-filter-card">
        <v-card-text
                class="age-slider">Select Age Range
          <div class="row">
            <div class="col-md-1">
              <v-text-field
                      id="min-age-text"
                      v-model="ageRange[0]"
                      class="mt-0 age-text-field"
                      hide-details
                      :min="minAge"
                      :max="ageRange[1]"
                      single-line
                      type="number"
              ></v-text-field>
            </div>
            <div class="col-md-8">
              <v-range-slider
                      v-model="ageRange"
                      :min="minAge"
                      :max="maxAge"
                      class="age-range-slider"
              ></v-range-slider>
            </div>
            <div class="col-md-1">
              <v-text-field
                      id="max-age-text"
                      v-model="ageRange[1]"
                      class="mt-0 age-text-field"
                      hide-details
                      :min="ageRange[0]"
                      :max="maxAge"
                      single-line
                      type="number"
              ></v-text-field>
            </div>

          </div>
        </v-card-text>

        <div class="row filters-card">
          <v-select
                  class="selector-input col-md-3"
                  label="Nationality"
                  :items="nationalities.names"
                  :value="nationality"
                  v-model="nationality"
          ></v-select>

          <v-select
                  class="selector-input col-md-3"
                  label="Gender"
                  :items="genders"
                  :value="gender"
                  v-model="gender"
          ></v-select>

          <v-select
                  class="selector-input col-md-3"
                  label="Traveller Type"
                  :items="travellerTypes.names"
                  :value="travellerType"
                  v-model="travellerType"
          ></v-select>

          <v-text-field
            class="selector-input col-md-3"
            label="Name" 
            v-model="name"
          >

          </v-text-field>
          <div class="search-buttons">
            <v-btn id="clearButton" color="secondary" depressed v-on:click="clearFilters" class="button button-card">
              Clear
            </v-btn>

            <v-btn id="searchButton" color="secondary" depressed @click="search()" class="button button-card">Search
            </v-btn>
          </div>
        </div>
      </v-card>

      <v-card elevation="10" class="table-card">
        <!-- Table Result -->
        <v-data-table
                :headers="headers"
                :items="travellers"
                class="elevation-1"
                :loading="isLoading"
                hide-actions
        >
          <template v-slot:items="props">
            <td class="text-xs-left" @click="$router.push(`/profile/${props.item.userId}`)">
              <v-avatar v-if="props.item.profilePhoto" class="profile-pic">
                <img class="avatar"
                     alt="Profile Photo"
                     :src="photoUrl(props.item.profilePhoto.photoId)">
              </v-avatar>
              <v-avatar  v-else class="profile-pic">
                <img alt="Profile Photo" src="../Profile/ProfilePic/defaultProfilePicture.png">
              </v-avatar>
            </td>
            <td class="text-xs-left" @click="$router.push(`/profile/${props.item.userId}`)">{{ props.item.firstName
              }}
            </td>
            <td class="text-xs-left" @click="$router.push(`/profile/${props.item.userId}`)">{{ props.item.middleName
              }}
            </td>
            <td class="text-xs-left" @click="$router.push(`/profile/${props.item.userId}`)">{{ props.item.lastName }}
            </td>
            <td class="text-xs-left" @click="$router.push(`/profile/${props.item.userId}`)">{{ props.item.age }}</td>
            <td class="text-xs-left" @click="$router.push(`/profile/${props.item.userId}`)">{{ props.item.gender }}
            </td>
            <td class="text-xs-left" @click="$router.push(`/profile/${props.item.userId}`)">
              <v-chip class="table-chip" v-for="nationality in props.item.nationalities"
                      v-bind:key="nationality.nationalityId">
                <CountryDisplay v-bind:country="nationality.nationalityCountry"></CountryDisplay>
              </v-chip>
            </td>
            <td class="text-xs-left" @click="$router.push(`/profile/${props.item.userId}`)">
              <v-chip class="table-chip" v-for="type in props.item.travellerTypes" v-bind:key="type">{{
                type }}
              </v-chip>
            </td>
          </template>
        </v-data-table>

        <v-spacer align="center">
          <v-icon color="secondary" class="pagination-arrow" @click="goBackOnePage" :disabled="pageIndex === 0">arrow_back</v-icon>
          <v-icon color="secondary" class="pagination-arrow" @click="goForwardOnePage" :disabled="travellers.length < pageLimit - 1">arrow_forward</v-icon>
        </v-spacer>
      </v-card>
    </div>
  </div>
</template>

<script>
  import {requestTravellers} from "./SearchTravellersService";
  import {getNationalities} from "../Profile/Nationalities/NationalityService";
  import {getAllTravellerTypes} from "../Profile/TravellerTypes/TravellerTypesService";
  import {endpoint} from "../../utils/endpoint";
  import moment from "moment";
  import CountryDisplay from "../../components/Country/CountryDisplay";

  const PAGE_LIMIT = 20;

  export default {
    components: {CountryDisplay},
    data() {
      return {
        nationalities: {
          names: [],
          ids: []
        },
        minAge: 13,
        maxAge: 115,
        ageRange: [13, 115],
        genders: ["Female", "Male", "Other"],
        travellerTypes: {
          names: [],
          ids: []
        },
        name: "",
        nationality: "",
        travellerType: "",
        gender: "",
        headers: [
          {text: 'Photo', align: 'left', sortable: false, value: 'profilePhoto'},
          {text: 'First Name', align: 'left', sortable: true, value: 'firstName'},
          {text: 'Middle Name', align: 'left', sortable: true, value: 'middleName'},
          {text: 'Last Name', align: 'left', sortable: true, value: 'lastName'},
          {text: 'Age', align: 'left', sortable: true, value: 'age'},
          {text: 'Gender', align: 'left', sortable: true, value: 'gender'},
          {text: 'Nationality', align: 'left', sortable: false, value: 'nationalityName'},
          {text: 'Traveller Type(s)', align: 'left', sortable: false, value: 'travellerTypes'},
        ],
        travellers: [],
        pageIndex: 0,
        pageLimit: PAGE_LIMIT,
        isLoading: false
      }
    },
    mounted: async function () {
      // Get all the current travellers
      this.search();

      // Get the nationalities
      let currentNationalities;
      try {
        currentNationalities = await getNationalities();

        for (let index = 0; index < currentNationalities.length; index++) {
          this.nationalities.names.push(currentNationalities[index].nationalityName);
          this.nationalities.ids.push(currentNationalities[index].nationalityId);
        }
      } catch (error) {
        this.showError("Could not get nationalities");
      }

      // Get all the traveller types
      let currentTravellerTypes;
      try {
        currentTravellerTypes = await getAllTravellerTypes();

        for (let index = 0; index < currentTravellerTypes.length; index++) {
          this.travellerTypes.names.push(currentTravellerTypes[index].travellerTypeName);
          this.travellerTypes.ids.push(currentTravellerTypes[index].travellerTypeId);
        }
      } catch (error) {
        this.showError("Could not get traveller types.");
      }
    },
    methods: {
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
       * Shows the snackbar error with the given error message
       */
      showError(errorMessage) {
        this.showSnackbar(errorMessage, "error", 3000);
      },
      /**
       * Searches for travellers in a paginated sense
       * @param {number | undefined} pageIndex - Either the new page to go to or undefined to reset page to 0
       */
      search: async function (pageIndex) {
        // get the queries from the selector variables
        // parse them into an acceptable format to be sent
        let queries = "";
        queries += "ageMin=" + moment().subtract(this.ageRange[0], "years");
        queries += "&ageMax=" + moment().subtract(this.ageRange[1], "years");
        queries += `&limit=${PAGE_LIMIT}`;

        if (this.gender !== "") {
          queries += "&gender=" + this.gender;
        }

        if (this.nationality !== "") {
          let nationalityIndex = this.nationalities.names.indexOf(this.nationality);
          queries += "&nationality=" + this.nationalities.ids[nationalityIndex];
        }

        if (this.travellerType !== "") {
          let typeIndex = this.travellerTypes.names.indexOf(this.travellerType);
          queries += "&travellerType=" + this.travellerTypes.ids[typeIndex];
        }

        if (this.name !== "") {
          queries += `&name=${this.name}`;
        }

        const pageToGoTo = pageIndex || 0;
        queries += `&offset=${pageToGoTo * PAGE_LIMIT}`;

        if (pageToGoTo === 0) {
          this.pageIndex = 0;
        } 

        try {
          // call the get travellers function passing in the formatted queries
          this.isLoading = true;
          const travellers = await requestTravellers(queries);
          this.isLoading = false;
          const userId = localStorage.getItem("userId");

          this.travellers = travellers
              .filter(traveller => traveller.userId !== Number(userId))
              .map(traveller => {
                const age = moment().diff(moment(traveller.dateOfBirth), "years");
                const nationalityNames = traveller.nationalities.map(nationality => nationality);
                const travellerTypes = traveller.travellerTypes.map(travellerType => travellerType.travellerTypeName);
                return {...traveller, age, nationalities: nationalityNames, travellerTypes}
              });
        } catch (error) {
          this.showError("Could not search for travellers")
        }
      },
      /**
       * Clears the filters age range, nationality, traveller type, gender and name
       */
      clearFilters: async function () {
        // Reset the selector variables to their default values
        this.ageRange = [13, 115];
        this.nationality = "";
        this.travellerType = "";
        this.gender = "";
        this.name = "";
        // Call the search function to get unfiltered results
        this.search(this.pageIndex);
      },
      /**
       * Gets the URL of a photo for a user
       * @param {number} photoId the ID of the photo to get
       * @returns {string} the url of the photo
       */
      photoUrl(photoId) {
        const authToken = localStorage.getItem("authToken");
        const queryAuthorization = `?Authorization=${authToken}`;
        return endpoint(`/users/photos/${photoId}${queryAuthorization}`);
      },
      /**
       * Goes back one page in the table
       */
      goBackOnePage() {
        this.pageIndex -= 1;
        this.search(this.pageIndex);
      },
      /**
       * Goes to the next page in the table
       */
      goForwardOnePage() {
        this.pageIndex += 1;
        this.search(this.pageIndex);
      }
    }
  }
</script>

<style lang="scss" scoped>
  @import "../../styles/_variables.scss";

  .search-container {
    margin: 20px 0 0;
  }

  .page-title {
    position: fixed;
    z-index: 1;
    width: 100%;
    padding: 15px;
    text-align: left;
    background: $primary;
    font-size: 20px;

    h1 {
      color: $darker-white;
    }
  }

  .title-card {
    display: inline-block;
  }

  .selector-input {
    padding: 0 0 0 10px;
    text-align: left;
  }

  .age-slider {
    padding: 10px 20% 0 20%;
    font-size: 16px;
  }

  .button {
    background-color: $primary;
    margin: auto;
  }

  .filters-card {
    margin: 10px;
    padding: 20px 20px 10px 20px;
  }

  .age-filter-card {
    margin: 10px;
  }

  .age-text-field {
    min-width: 50px;
  }

  .age-range-slider {
    padding: 0 0 0 10px;
  }

  .table-chip {
    background-color: $primary;
    color: $darker-white;
  }

  .button-card {
    padding: 10px;
    margin: 5px;
  }

  .table-card {
    margin: 10px;
  }

  .profile-pic {
    width: 30%;
    height: auto;
  }

  .pagination-arrow {
    font-size: 2.5rem;
    cursor: pointer;
    margin-left: 10px;
    margin-right: 10px;
  }

  .search-buttons {
    margin-left: auto;
  }

  .avatar {
    object-fit: cover;
  }

</style>
