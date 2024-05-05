package com.faforever.commons.replay.body.event;

import lombok.Getter;

@Getter
public enum EventCommandType {
  // Order is crucial
  NONE("NONE"),
  STOP("Stop"),
  MOVE("Move"),
  DIVE("Dive"),
  FORM_MOVE("FormMove"),
  BUILD_SILO_TACTICAL("BuildSiloTactical"),
  BUILD_SILO_NUKE("BuildSiloNuke"),
  BUILD_FACTORY("BuildFactory"),
  BUILD_MOBILE("BuildMobile"),
  BUILD_ASSIsT("BuildAssist"),
  ATTACK("Attack"),
  FORM_ATTACK("FormAttack"),
  NUKE("Nuke"),
  TACTICAL("Tactical"),
  TELEPORT("Teleport"),
  GUARD("Guard"),
  PATROL("Patrol"),
  FERRY("Ferry"),
  FORM_PATROL("FormPatrol"),
  RECLAIM("Reclaim"),
  REPAIR("Repair"),
  CAPTURE("Capture"),
  TRANSPORT_LOAD_UNITS("TransportLoadUnits"),
  TRANSPORT_REVERSE_LOAD_UNITS("TransportReverseLoadUnits"),
  TRANSPORT_UNLOAD_UNITS("TransportUnloadUnits"),
  TRANSPORT_UNLOAD_SPECIFIC_UNITS("TransportUnloadSpecificUnits"),
  DETACH_FROM_TRANSPORT("DetachFromTransport"),
  UPGRADE("Upgrade"),
  SCRIPT("Script"),
  ASSIST_COMMANDER("AssistCommander"),
  KILL_SELF("KillSelf"),
  DESTROY_SELF("DestroySelf"),
  SACRIFICE("Sacrifice"),
  PAUSE("Pause"),
  OVER_CHARGE("OverCharge"),
  AGGRESSIVE_MOVE("AggressiveMove"),
  FORM_AGGRESSIVE_MOVE("FormAggressiveMove"),
  ASSIST_MOVE("AssistMove"),
  SPECIAL_ACTION("SpecialAction"),
  DOCK("Dock");

  private final String string;

  EventCommandType(String string) {
    this.string = string;
  }
}
