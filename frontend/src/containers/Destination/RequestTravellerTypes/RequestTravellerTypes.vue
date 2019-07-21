<template>
  <v-dialog
    v-model="isDialogShowing"
    width="60%"
    persistent
  >
    <v-card id="request-traveller-types-card">
      <v-card-title class="primary title">
        <v-layout row>
          <v-spacer align="center">
            <h2 class="light-text">
              <v-icon
                large
                class="light-text"
              >directions_walk</v-icon>
              Request Traveller Types
            </h2>
          </v-spacer>
        </v-layout>
      </v-card-title>
      <v-form
        ref="form" 
      >
      <v-layout row style="padding: 15px 0 15px 0;">
        <v-flex xs8 offset-xs2>
        <v-combobox
          v-model="selectedTravellerTypes"
          :items="travellerTypes"
          item-text="travellerTypeName"
          item-value="travellerTypeId"
          :rules="[rules.requiredArray]"
          label="Traveller Types"
          chips
          clearable
          solo
          multiple
        >

          <template v-slot:selection="data">
            <v-chip
              color="primary"
              text-color="white"
              :selected="data.selected"
              close
              @input="removeTravellerType(data.item)"
            >
              <strong>{{ data.item.travellerTypeName }}</strong>&nbsp;
            </v-chip>
          </template>
        </v-combobox>
        </v-flex>
      </v-layout>
      </v-form>

      <v-card-actions>
        <v-spacer align="right">
          <v-btn
            flat
            color="error"
            @click="isDialogShowing = false"
          >Cancel</v-btn>
          <v-btn
            flat
            color="success"
            @click="sendProposal"
            :loading="isLoading"
          >Submit</v-btn>
        </v-spacer>
      </v-card-actions>

    </v-card>
  </v-dialog>
</template>

<script>
import { getTravellerTypes, sendProposal } from "./RequestTravellerTypesService";
import { rules } from "../../../utils/rules";

export default {
  props: {
    isShowingTravellerTypesDialog: {
      type: Boolean,
      required: true
    },
    destination: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      isDialogShowing: false,
      travellerTypes: [],
      selectedTravellerTypes: [],
      isLoading: false,
      rules
    };
  },
  async mounted() {
    const travellerTypes = await getTravellerTypes();
    this.travellerTypes = travellerTypes;
  },
  methods: {
    async sendProposal() {
      const validForm = this.$refs.form.validate();
      if (!validForm) return;
      try {
        const destinationId = this.destination.destinationId;
        const travellerTypeIds = this.selectedTravellerTypes
                                  .map(selectedTravellerType => selectedTravellerType.travellerTypeId);

        
        await sendProposal(destinationId, travellerTypeIds); 
        this.isDialogShowing = false;
        this.$emit("proposalSent");
      } catch (e) {
        console.log(e);
        this.$emit("showError", "Could not send proposal");
      }
    } 
  },
  watch: {
    // Synchronize the dialog state with the props
    isDialogShowing(newValue) {
      this.$emit("update:isShowingTravellerTypesDialog", newValue);
    },
    isShowingTravellerTypesDialog(newValue) {
      this.isDialogShowing = newValue;
    }
  }
};
</script>

<style lang="scss" scoped>
@import "../../../styles/_variables.scss";

#content {
}

.light-text {
  color: $darker-white;
}
</style>



