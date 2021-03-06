package modules.voice;

import com.google.inject.ImplementedBy;

import java.util.concurrent.CompletionStage;

/**
 * Defines generic api to talk to a voice server. Specifies the default implementation to use
 */
@ImplementedBy(JanusServerApi.class)
public interface VoiceServerApi {
  /**
   * Checks if a room exists by it's room ID
   * @param roomId The room ID to check
   * @param sessionId The session to the janus server
   * @param pluginHandle The Id of the audio room plugin
   * @return Tru if the room exists, false otherwise
   */
  CompletionStage<Boolean> checkRoomExists(long roomId, long sessionId, long pluginHandle);

  /**
   * Generates a new room. Will be called when a room doesn't exist
   * @param token The token to authenticate the room with
   * @return The room ID that was generated
   */
  CompletionStage<Long> generateRoom(String token, long sessionId, long pluginHandle);
}
