from datetime import date, datetime
from pydantic import BaseModel, Field


class UserCreate(BaseModel):
    username: str = Field(min_length=3, max_length=64)
    password: str = Field(min_length=6, max_length=128)


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"


class StepEntry(BaseModel):
    device_id: str
    day: date
    steps: int = Field(ge=0)


class StepSyncRequest(BaseModel):
    entries: list[StepEntry]


class StepRecordOut(BaseModel):
    device_id: str
    day: date
    steps: int
    synced_at: datetime

    class Config:
        from_attributes = True
