package modules.voice.Requests;

import com.google.inject.ImplementedBy;
import util.Security;

/**
 * Voice request is used to define different types of requests to make with the voice plugin
 */
public interface JanusVoiceRequest {
  /**
   * Every request has a string identifier representing what to request
   * @return The request
   */
   String getRequest();
}
