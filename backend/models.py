from sqlalchemy import (
    Column, Integer, String, Date, DateTime, ForeignKey, UniqueConstraint, func
)
from sqlalchemy.orm import relationship
from database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(64), unique=True, nullable=False, index=True)
    password_hash = Column(String(255), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    step_records = relationship("StepRecord", back_populates="user", cascade="all, delete-orphan")


class StepRecord(Base):
    __tablename__ = "step_records"
    __table_args__ = (
        UniqueConstraint("user_id", "device_id", "day", name="uix_user_device_day"),
    )

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    device_id = Column(String(128), nullable=False)
    day = Column(Date, nullable=False)
    steps = Column(Integer, nullable=False)
    synced_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    user = relationship("User", back_populates="step_records")
