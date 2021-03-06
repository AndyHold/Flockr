<template>
  <v-card flat>
    <v-form ref="form">

      <v-text-field
        class="padding"
        label="Chat name"
        :rules="requiredRule"
        v-model="selectedChatName"
      ></v-text-field>

        <GenericCombobox
          class="padding"
          label="Users"
          :get-function="searchUser"
          :item-text="(user) => user.firstName + ' ' + user.lastName"
          multiple
          v-model="selectedUsers"
          @items-selected="updateSelectedUsers"
        ></GenericCombobox>

      <v-spacer align="center">
        <v-btn
          color="secondary"
          dark
          v-on:click="createNewChat"
          depressed
          >
          <v-icon>add</v-icon>
        </v-btn>
      </v-spacer>
    </v-form>
  </v-card>
</template>

<script>
  import { getUsers } from "../ChatService";
  import { rules } from "../../../../utils/rules";
  import GenericCombobox from "../../../../components/GenericCombobox/GenericCombobox";
import UserStore from '../../../../stores/UserStore';

  export default {
    name: "CreateChat",
      components: {GenericCombobox},
      props: {
      isShowing: {
        type: Boolean,
        required: false
      },
    },
    data() {
      return {
        allUsers: [],
        selectedChatName: "",
        selectedUsers: [],
        requiredRule: [rules.required],
        arrayRule: [rules.requiredArray]
      };
    },
    methods: {
      /**
       * Updates the users to the new selected users
       */
      updateSelectedUsers(newUsers) {
          this.selectedUsers = newUsers
      },
      /**
       * Allows the user to search for other user names to add in the chat
       */
      searchUser: async name => {
        const allUsers = await getUsers(name);
        return allUsers.filter(user => {
          return user.userId !== UserStore.data.userId;
        });
      },
      /**
       * Retrieve all users in the system.
       * Filter out the user's own id from the list so they can't add themselves to the chat.
       */
      async getAllUsers() {
        const allUsers = await getUsers();
        this.allUsers = allUsers.filter(user => user.userId !== UserStore.data.userId);
      },
      /**
       * Called when the user clicks the create chat button.
       * Checks that the fields have been filled in then emits to the parent component
       * which sends the request to create the chat.
       */
      async createNewChat() {
        if (!this.$refs.form.validate()) {
          return
        }

        if (!this.selectedUsers.length) {
          this.showSnackbar("Please select at least one user", "error", 2000);
          return;
        }

        try {
          const userIds = this.selectedUsers.map(user => user.userId);
          this.$emit("createChat", userIds, this.selectedChatName);
        } catch (e) {
          this.showSnackbar(e, "error", 2000);
        }
      },
      /**
       * Shows a snackbar based on the message, colour and timeout.
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
    },
    watch: {
      isShowing(value) {
        this.isShowing = value;
      }
    }
  }
</script>

<style scoped>

.padding {
  margin: 20px;
  padding: 20px;
}

</style>