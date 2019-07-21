<template>
  <v-navigation-drawer permanent id="navbar" :width="240">
    <v-list dense class="pt-0">
      <v-list-tile
        v-for="item in itemsToShow"
        :key="item.title"
        @click="navClicked(item.url)"
        class="nav-item"
      >
        <v-list-tile-action>
          <v-icon class="nav-icon">{{ item.icon }}</v-icon>
        </v-list-tile-action>

        <v-list-tile-content>
          <v-list-tile-title>{{ item.title }}</v-list-tile-title>
        </v-list-tile-content>
      </v-list-tile>


      <v-list-tile
        @click="goBackToOwnAccount()"
        class="nav-item"
        v-if="userStore.viewingAsAnotherUser"
      >
        <v-list-tile-action>
          <v-icon class="nav-icon">arrow_back</v-icon>
        </v-list-tile-action>

        <v-list-tile-content>
          <v-list-tile-title>Back to own account</v-list-tile-title>
        </v-list-tile-content>
      </v-list-tile>




    </v-list>
 </v-navigation-drawer>
</template>

<script>

import UserStore from "../../../stores/UserStore";
import { logout } from "./SidebarService";

export default {
  data() {
    return {
      userStore: UserStore.data,
      items: [
        {
          title: "Home",
          url: "/",
          icon: "dashboard",
          loggedIn: true,
          loggedOut: true,
          requiresAdminRole: false
        },
        {
          title: "Search Travellers",
          icon: "search",
          url: "/search",
          profileCompleted: true,
          loggedIn: true,
          loggedOut: false,
          requiresAdminRole: false
        },
        {
          title: "Destinations",
          icon: "location_on",
          url: "/destinations",
          profileCompleted: true,
          loggedIn: true,
          loggedOut: false,
          requiresAdminRole: false
        },
        {
          title: "Trips",
          icon: "navigation",
          url: "/trips",
          profileCompleted: true,
          loggedIn: true,
          loggedOut: false,
          requiresAdminRole: false
        },
        {
          title: "Sign up",
          icon: "person_add",
          url: "/signup",
          loggedIn: false,
          loggedOut: true,
          requiresAdminRole: false
        },
        {
          title: "Log in",
          icon: "exit_to_app",
          url: "/login",
          loggedIn: false,
          loggedOut: true,
          requiresAdminRole: false
        },
        {
          title: "Profile",
          icon: "face",
          url: "/profile",
          loggedIn: true,
          loggedOut: false,
          requiresAdminRole: false
        },
        {
          title: "Log out",
          url: "/logout",
          icon: "power_settings_new",
          loggedIn: true,
          loggedOut: false,
          requiresAdminRole: false
        },
        {
          title: "Admin Panel",
          url: "/admin",
          icon: "how_to_reg",
          loggedIn: true,
          loggedOut: false,
          requiresAdminRole: true
        }
      ],
    };
  },
  methods: {

    /**
     * Run when the nav was clicked on
     * @param {string} url - The url of nav item that was clicked on
     */
    async navClicked(url) {
      switch (url) {
        case "/profile":
          this.$router.push(`/profile/${UserStore.data.userId}`);
          break;
        case "/logout":
          await logout();
          UserStore.methods.logout();
          this.$router.push("/");
          break;
        default:
          this.$router.push(url);
          break;
      }
    },
    /**
     * Allows an admin to go back to their own account
     */
    async goBackToOwnAccount() {
      await UserStore.methods.backToOwnAccount();
      this.$router.push(`/profile/${UserStore.data.userId}`);
    }
  },
  computed: {
    /**
     * Computed property which filters nav items to show
     */
    itemsToShow() {
      const loggedIn = UserStore.methods.loggedIn();
      const profileCompleted = UserStore.methods.profileCompleted();
      const isAdmin = UserStore.methods.isAdmin();

      return this.items.filter(item => {
        if (item.loggedIn && loggedIn) {
          if ((item.profileCompleted && profileCompleted) || !item.profileCompleted) {
            if (item.requiresAdminRole && !isAdmin) {
              return false;
            } else {
              return true;
            }
          } else {
            return false
          }
        } else if (item.loggedOut && !loggedIn) {
          return true;
        }
        else {
          return false;
        }
      });
    }
  },
}
</script>

<style lang="scss" scoped>
  @import "../../../styles/_variables.scss";

  #navbar {
    background-color: $primary;
    position: fixed !important;
    top: 64px;
  }

  #title-box {
    background-color: $secondary;
  }

  #title {
    color:$darker-white;  
  }

  .nav-icon {
    color:$darker-white;  
  }

  .nav-item {
    color: $darker-white;
  }

  .light-divider {
    background-color: $lighter-primary;
  }

</style>

