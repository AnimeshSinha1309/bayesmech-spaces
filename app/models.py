from typing import Any, Literal

from pydantic import BaseModel, Field


class Connection(BaseModel):
    user_id: str
    dm_thread_id: str
    connection_type: Literal["explicit_chat"] = "explicit_chat"
    last_interaction_at: str | None = None


class EventRefs(BaseModel):
    hosted_event_ids: list[str] = Field(default_factory=list)
    joined_event_ids: list[str] = Field(default_factory=list)
    attended_event_ids: list[str] = Field(default_factory=list)


class UserCreate(BaseModel):
    email: str
    password: str
    display_name: str
    username: str | None = None
    avatar_url: str | None = None
    auth_providers: list[str] = Field(default_factory=lambda: ["email"])
    google_id: str | None = None


class UserGoogleAuth(BaseModel):
    email: str
    google_id: str
    display_name: str
    username: str | None = None
    avatar_url: str | None = None


class UserSignIn(BaseModel):
    email: str
    password: str


class UserPatch(BaseModel):
    display_name: str | None = None
    username: str | None = None
    avatar_url: str | None = None
    account_status: str | None = None
    persona: dict[str, Any] | None = None
    connections: list[Connection] | None = None
    event_refs: EventRefs | None = None


class UserDocument(BaseModel):
    id: str = Field(alias="_id")
    email: str
    password: str
    auth_providers: list[str]
    google_id: str | None = None
    display_name: str
    username: str | None = None
    avatar_url: str | None = None
    account_status: str = "active"
    persona: dict[str, Any] = Field(default_factory=dict)
    connections: list[Connection] = Field(default_factory=list)
    event_refs: EventRefs = Field(default_factory=EventRefs)
    main_thread_id: str
    last_login_at: str | None = None


class EventCreate(BaseModel):
    creator_user_id: str
    title: str
    description: str
    category_tags: list[str] = Field(default_factory=list)
    visibility_type: Literal["targeted", "public", "invite_only"] = "targeted"
    created_via: Literal["chat", "manual", "admin"] = "chat"
    timing: dict[str, Any]
    location: dict[str, Any]
    capacity: dict[str, Any] | None = None
    routing: dict[str, Any] | None = None
    source: dict[str, Any] | None = None


class EventPatch(BaseModel):
    title: str | None = None
    description: str | None = None
    category_tags: list[str] | None = None
    status: str | None = None
    visibility_type: str | None = None
    timing: dict[str, Any] | None = None
    location: dict[str, Any] | None = None
    capacity: dict[str, Any] | None = None
    attendance: dict[str, Any] | None = None
    routing: dict[str, Any] | None = None
    chat: dict[str, Any] | None = None
    source: dict[str, Any] | None = None


class EventDocument(BaseModel):
    id: str = Field(alias="_id")
    creator_user_id: str
    title: str
    description: str
    category_tags: list[str] = Field(default_factory=list)
    status: str = "published"
    visibility_type: str = "targeted"
    created_via: str = "chat"
    timing: dict[str, Any]
    location: dict[str, Any]
    capacity: dict[str, Any] = Field(default_factory=dict)
    attendance: dict[str, Any] = Field(default_factory=dict)
    routing: dict[str, Any] = Field(default_factory=dict)
    chat: dict[str, Any]
    source: dict[str, Any] = Field(default_factory=dict)
    created_at: str
    updated_at: str


class EventMembershipCreate(BaseModel):
    user_id: str
    role: Literal["creator", "host", "attendee"] = "attendee"
    rsvp_status: Literal["invited", "suggested", "joined", "waitlisted", "declined", "attended", "no_show", "left"] = "joined"
    discovery_source: Literal["direct_chat", "broadcast", "search", "friend", "admin"] = "broadcast"


class DirectMessageCreate(BaseModel):
    user_id: str
    other_user_id: str


class ChatMessageCreate(BaseModel):
    sender_type: Literal["user", "system", "assistant"]
    sender_user_id: str | None = None
    message_type: Literal["text", "event_card", "system_notice"] = "text"
    content_text: str
    content_structured: dict[str, Any] | None = None


class ProfileAiMessage(BaseModel):
    role: Literal["assistant", "user"]
    text: str


class ProfileAiTurnRequest(BaseModel):
    current_profile_dict: dict[str, Any] = Field(default_factory=dict)
    transcript: list[ProfileAiMessage] = Field(default_factory=list)
    user_message: str | None = None
    display_name: str | None = None


class ProfileAiTurnResponse(BaseModel):
    assistant_text: str
    updated_profile_dict: dict[str, Any] = Field(default_factory=dict)
    is_complete: bool
    model: str


class ProfileAiEndResponse(BaseModel):
    final_profile_dict: dict[str, Any] = Field(default_factory=dict)
    closing_text: str
    is_complete: bool
    model: str
