<template>
  <div
      id="root-container"
      v-if="userProfile"
  >
    <v-alert
        :value="shouldShowBanner()"
        color="info"
        icon="info"
    >
      Please fill in your full profile before using the site
    </v-alert>
        <v-card>
          <cover-photo
              :userProfile="userProfile"
              :photos="userProfile.personalPhotos"
              v-on:updateProfilePic="updateProfilePic"
              v-on:showError="showError"
              id="cover-photo"
              @deleteCoverPhoto="deleteCoverPhoto"
              ref="cover-photo"
              @coverPhotoUpdated="coverPhotoUpdated"/>

          <ProfilePic
              :profilePhoto="userProfile.profilePhoto"
              :photos="userProfile.personalPhotos"
              :userId="userProfile.userId"
              v-on:updateProfilePic="updateProfilePic"
              v-on:showError="showError"
              :fullname="fullname"
              class="profile-pic"
          />


          <v-card-title primary-title>

            <div class="col-lg-5">
              <v-card v-if="shouldSeeUndoRedo" id="undo-redo-card">
                <p>You can undo and redo your changes.</p>

                <UndoRedo ref="undoRedo"/>
              </v-card>

              <BasicInfo
                  :userProfile="userProfile"
                  @update-basic-info="this.updateBasicInfo"
              />
            </div>
            <div class="col-lg-7">
              <Nationalities
                  :userNationalities="userProfile.nationalities"
                  @update-user-nationalities="updateUserNationalities"
                  :userId="userProfile.userId"
              />
              <Passports
                  :userPassports="userProfile.passports"
                  @update-user-passports="updateUserPassports"
                  :userId="userProfile.userId"
              />
              <TravellerTypes
                  :userTravellerTypes="userProfile.travellerTypes"
                  @update-user-traveller-types="updateUserTravellerTypes"
                  :userId="userProfile.userId"
              />
            </div>

            <v-flex sm12>
              <Photos
                  :photos="userProfile.personalPhotos"
                  @deletePhoto="deletePhoto"
                  @undoDeletePhoto="undoDeletePhoto"
                  @addPhoto="addImage"
                  @showError="showError"
                  @addPhotoCommand="addPhotoCommand"/>
            </v-flex>

            <v-flex sm12>
              <div>

                <h3 class="trips-header headline">Trips</h3>
                <v-card class="trips-card">

                  <Trips
                      :trips.sync="userProfile.trips"
                      :profile-id="profileId"
                      viewOnly
                  />
                </v-card>
              </div>
            </v-flex>

          </v-card-title>
        </v-card>
  </div>
</template>

<script>
  import ProfilePic from "./ProfilePic/ProfilePic";
  import Nationalities from "./Nationalities/Nationalities";
  import Passports from "./Passports/Passports";
  import TravellerTypes from "./TravellerTypes/TravellerTypes";
  import BasicInfo from "./BasicInfo/BasicInfo";
  import Trips from "../Trips/Trips";
  import Photos from "./Photos/Photos";
  import UndoRedo from "../../components/UndoRedo/UndoRedo";
  import Command from "../../components/UndoRedo/Command";
  import UserStore from "../../stores/UserStore";
  import moment from "moment";
  import {
    getUser,
    requestChangeCoverPhoto,
    requestDeleteCoverPhoto,
    requestUndoDeleteCoverPhoto
  } from "./ProfileService";
  import {updateBasicInfo} from "./BasicInfo/BasicInfoService";
  import {updateNationalities} from "./Nationalities/NationalityService";
  import {updatePassports} from "./Passports/PassportService";
  import {updateTravellerTypes} from "./TravellerTypes/TravellerTypesService";
  import {setProfilePictureToOldPicture} from "./ProfilePic/ProfilePicService";
  import {endpoint} from "../../utils/endpoint";
  import {deleteUserPhoto, undoDeleteUserPhoto} from '../UserGallery/UserGalleryService';
  import CoverPhoto from "./CoverPhoto/CoverPhoto";

  export default {
    components: {
      CoverPhoto,
      ProfilePic,
      Nationalities,
      Passports,
      BasicInfo,
      TravellerTypes,
      Trips,
      Photos,
      UndoRedo
    },
    data() {
      return {
        userProfile: null,
        photos: null,
        profileId: this.$route.params.id
      };
    },
    mounted() {
      this.getUserInfo();
      console.log(this.profileId);
    },
    methods: {
      /**
       * Changes a cover photo and sets the undo and redo commands.
       *
       * @param {Object} newCoverPhoto the new cover photo to update.
       */
      coverPhotoUpdated(newCoverPhoto) {
        const oldCoverPhoto = {...this.userProfile.coverPhoto};

        this.userProfile.coverPhoto = newCoverPhoto;

        const undoCommand = async () => {
          let message = "Cover photo successfully reverted";
          let color = "success";
          try {
            if (oldCoverPhoto) {
              this.userProfile.coverPhoto = await requestChangeCoverPhoto(this.userProfile.userId,
                  oldCoverPhoto.photoId);
            } else {
              await requestDeleteCoverPhoto(this.userProfile.userId);
            }
          } catch (e) {
            message = "Cover photo could not be reverted";
            color = "error";
          }
          this.$root.$emit("show-snackbar", {
            timeout: 3000,
            color: color,
            message: message
          });
        };

        const redoCommand = async () => {
          let message = "Cover photo changed successfully";
          let color = "success";
          try {
            this.userProfile.coverPhoto = await requestChangeCoverPhoto(this.userProfile.userId, newCoverPhoto.photoId);
          } catch (e) {
            message = "Could not change cover photo";
            color = "error";
          }
          this.$root.$emit("show-snackbar", {
            timeout: 3000,
            color: color,
            message: message
          });
        };

        const changeCoverPhotoCommand = new Command(undoCommand, redoCommand);
        this.$refs.undoRedo.addUndo(changeCoverPhotoCommand);
      },
      /**
       * Deletes a cover photo and sets the undo and redo commands.
       */
      async deleteCoverPhoto() {
        try {
          await requestDeleteCoverPhoto(this.userProfile.userId);
          const photoId = this.userProfile.coverPhoto.photoId;
          this.userProfile.coverPhoto = null;
          this.$refs["cover-photo"].closeCoverPhotoDialog();

          const undoCommand = async () => {
            try {
              this.userProfile.coverPhoto = await requestUndoDeleteCoverPhoto(this.userProfile.userId, photoId);
            } catch (error) {
              this.$root.$emit("show-snackbar", {
                timeout: 4000,
                color: "error",
                message: "Error undoing cover photo deletion"
              });
            }
          };
          let redoCommand = async () => {
            try {
              await requestDeleteCoverPhoto(this.userProfile.userId);
              this.userProfile.coverPhoto = null;
            } catch (error) {
              this.$root.$emit("show-snackbar", {
                timeout: 4000,
                color: "error",
                message: "Error redoing cover photo deletion"
              });
            }
          };

          const deleteCoverPhotoCommand = new Command(undoCommand, redoCommand);
          this.$refs.undoRedo.addUndo(deleteCoverPhotoCommand);
        } catch (error) {
          this.$root.$emit("show-snackbar", {
            timeout: 4000,
            color: "error",
            message: "Error deleting cover photo" //TODO: do this properly.
          });
        }
      },
      /**
       * Add an undo/redo command to the stack.
       *
       * @param {Command} command the command to add to the undo/redo panel.
       * */
      addPhotoCommand(command) {
        this.$refs.undoRedo.addUndo(command);
      },
      /**
       * Updates a users traveller types and sets undo/redo commands.
       *
       * @param oldTravellerTypes the old traveller types of the user.
       * @param newTravellerTypes the new traveller types to set.
       */
      updateUserTravellerTypes(oldTravellerTypes, newTravellerTypes) {
        const userId = localStorage.getItem("userId");

        const command = async (travellerTypes) => {
          const travellerTypeIds = travellerTypes.map(t => t.travellerTypeId);
          await updateTravellerTypes(userId, travellerTypeIds);
          UserStore.data.travellerTypes = travellerTypes;
          this.userProfile.travellerTypes = travellerTypes;
        };

        const undoCommand = command.bind(null, oldTravellerTypes);
        const redoCommand = command.bind(null, newTravellerTypes);
        const updateTravellerTypesCommand = new Command(undoCommand, redoCommand);
        this.$refs.undoRedo.addUndo(updateTravellerTypesCommand);
        redoCommand(); // perform update
      },
      /**
       * Updates a users passports and sets undo/redo commands.
       *
       * @param oldPassports the old passports of the user.
       * @param newPassports the new passport to update.
       */
      updateUserPassports(oldPassports, newPassports) {
        const userId = localStorage.getItem("userId");

        const command = async (passports) => {
          const passportIds = passports.map(p => p.passportId);
          await updatePassports(userId, passportIds);
          UserStore.data.passports = passports;
          this.userProfile.passports = passports;
        };

        const undoCommand = command.bind(null, oldPassports);
        const redoCommand = command.bind(null, newPassports);
        const updatePassportsCommand = new Command(undoCommand, redoCommand);
        this.$refs.undoRedo.addUndo(updatePassportsCommand);
        redoCommand(); // actually perform the update
      },
      /**
       * Updates a users nationalities and sets undo/redo commands.
       *
       * @param oldNationalities the old nationalities of the user.
       * @param newNationalities the new nationalities to set.
       */
      updateUserNationalities(oldNationalities, newNationalities) {
        const userId = localStorage.getItem("userId");

        const command = async (nationalities) => {
          const nationalityIds = nationalities.map(nationality => nationality.nationalityId);
          await updateNationalities(userId, nationalityIds);
          UserStore.data.nationalities = nationalities;
          this.userProfile.nationalities = nationalities;
        };

        const undoCommand = command.bind(null, oldNationalities);
        const redoCommand = command.bind(null, newNationalities);
        const updateNationalitiesCommand = new Command(undoCommand, redoCommand);
        this.$refs.undoRedo.addUndo(updateNationalitiesCommand);
        redoCommand(); // perform the update
      },
      /**
       * Updates the basic info of a user and sets undo/redo commands.
       *
       * @param oldBasicInfo the old basic info of the user.
       * @param newBasicInfo the new basic info of the user.
       */
      updateBasicInfo(oldBasicInfo, newBasicInfo) {
        const userId = localStorage.getItem("userId");
        const {userProfile} = this;

        const command = async (basicInfo) => {
          await updateBasicInfo(userId, basicInfo);
          const mergedUserProfile = {...userProfile, ...basicInfo};
          UserStore.methods.setData(mergedUserProfile);
          this.userProfile = mergedUserProfile;
        };

        const undoCommand = command.bind(null, oldBasicInfo);
        const redoCommand = command.bind(null, newBasicInfo);
        const updateBasicInfoCommand = new Command(undoCommand, redoCommand);
        this.$refs.undoRedo.addUndo(updateBasicInfoCommand);
        redoCommand(newBasicInfo); // actually make the update
      },
      /**
       * @param {String} message the message to show in the snackbar
       * @param {String} color the colour for the snackbar
       * @param {Number} timeout the amount of time (in ms) for which we show the snackbar
       */
      showSnackbar(message, color, timeout) {
        this.$root.$emit({
          message: message,
          color: color,
          timeout: timeout
        });
      },
      /**
       * Called when a deletePhoto event is emitted from the photos component.
       * Removes the photo at the given index.
       *
       * @param {Number} index the index of the photo to be removed.
       * @param {Boolean} shouldShowSnackbar true if the snackbar should be shown.
       */
      deletePhoto(index, shouldShowSnackbar) {
        this.userProfile.personalPhotos.splice(index, 1);
        if (shouldShowSnackbar) {
          this.showSnackbar("Photo deleted successfully", "success", 3000);
        }
      },
      /**
       * Called when a undoDeletePhoto event is emitted from the photos component.
       * Adds the photo back to the list at the given index.
       *
       * @param {Number} index the index where the photo should be added.
       * @param {Object} photo the photo to add.
       */
      undoDeletePhoto(index, photo) {
        this.userProfile.personalPhotos.splice(index, 0, photo);
      },
      /**
       * Gets a users info and sets the users state
       */
      async getUserInfo() {
        const userId = this.$route.params.id;

        const user = await getUser(userId);

        // Change date format so that it displays on the basic info component.
        user.dateOfBirth = user.dateOfBirth
            ? moment(user.dateOfBirth).format("YYYY-MM-DD")
            : "";

        this.userProfile = user;
      },
      /**
       * Decides when the incomplete user banner should display.
       */
      shouldShowBanner() {
        return !(
            this.userProfile.firstName &&
            this.userProfile.lastName &&
            this.userProfile.gender &&
            this.userProfile.dateOfBirth &&
            this.userProfile.nationalities.length &&
            this.userProfile.travellerTypes.length
        );
      },
      /**
       * Updates the profile picture of a user after it has been changed.
       * Add an undo/redo command to the undo/redo stack.
       * @param oldPhoto the old profile picture.
       * @param newPhoto the new profile picture.
       */
      updateProfilePic(oldPhoto, newPhoto) {
        this.userProfile.profilePhoto = newPhoto;
        let undoCommand = async (profilePhoto) => {
          if (profilePhoto) {
            await setProfilePictureToOldPicture(profilePhoto);
          }
          this.userProfile.profilePhoto = profilePhoto;
        };
        undoCommand = undoCommand.bind(null, oldPhoto);

        let redoCommand = async (profilePhoto) => {
          await setProfilePictureToOldPicture(profilePhoto);
          this.userProfile.profilePhoto = profilePhoto;
        };
        redoCommand = redoCommand.bind(null, newPhoto);
        const updateProfilePicCommand = new Command(undoCommand, redoCommand);
        this.$refs.undoRedo.addUndo(updateProfilePicCommand);
      },
      /**
       * Shows an snackbar error message
       * @param {string} text the text to display on the snackbar
       */
      showError(text) {
        this.showSnackbar(text, "error", 3000);
      },
      /**
       * Update an image in the front end and create and store undo/redo commands
       * for it.
       *
       * @param image the image to update.
       */
      addImage(image) {
        image.endpoint = endpoint(
            `/users/photos/${image["photoId"]}?Authorization=${localStorage.getItem("authToken")}`);
        image.thumbEndpoint = endpoint(
            `/users/photos/${image["photoId"]}/thumbnail?Authorization=${localStorage.getItem(
                "authToken")}`);
        this.userProfile.personalPhotos.push(image);

        const undoCommand = (
            async (image) => {
              await deleteUserPhoto(image);
              this.undoAddPhoto(image);
            }
        ).bind(null, image);
        const redoCommand = (
            async (image) => {
              await undoDeleteUserPhoto(image);
              image.endpoint = endpoint(
                  `/users/photos/${image["photoId"]}?Authorization=${localStorage.getItem(
                      "authToken")}`);
              image.thumbEndpoint = endpoint(
                  `/users/photos/${image["photoId"]}/thumbnail?Authorization=${localStorage.getItem(
                      "authToken")}`);
              this.userProfile.personalPhotos.push(image);
            }
        ).bind(null, image);

        const undoUploadCommand = new Command(undoCommand, redoCommand);
        this.$refs.undoRedo.addUndo(undoUploadCommand);
      },
      /**
       * Undo function for adding an image.
       *
       * @param image the image to undo.
       */
      undoAddPhoto(image) {
        this.userProfile.personalPhotos = this.userProfile.personalPhotos.filter(
            e => e.photoId !== image.photoId);
      }
    },
    computed: {
      /**
       * Gets the full name of the user.
       *
       * @return {string} the full name of the user.
       */
      fullname() {
        return `${this.userProfile.firstName} ${this.userProfile.lastName}`;
      },
        /**
         * Return whether to see the undo / redo component
         * @returns {boolean} true if should see the undo/redo component, false otherwise
         */
      shouldSeeUndoRedo() {
        return this.userProfile.userId === UserStore.data.userId;
      }
    }
  };
</script>

<style lang="scss" scoped>
  @import "../../styles/_variables.scss";

  #root-container {
    width: 100%;
    margin-right: 15px;

    #undo-redo-card {
      margin-top: 10px;
      padding: 5px;
      display: flex;
      flex-direction: column;
      justify-content: flex-start;
      align-items: center;
    }
  }

  .trips-header {
    text-align: left;
    margin-bottom: 7px;
    margin-top: 30px;
  }

  .trips-card {
    max-height: 350px;
    overflow-y: auto;
  }

  .top-panel {
    margin-top: 75px;
  }


  .profile-pic {
    position: absolute;
    left: 30px;
    top: 40px;
  }

  #cover-photo {
    margin-bottom: 75px;
  }
</style>


