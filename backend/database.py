import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from dotenv import load_dotenv

load_dotenv()

# Example: postgresql://pedometer_user:secret@localhost:5432/pedometer_db
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://pedometer_user:secret@localhost:5432/pedometer_db",
)

engine = create_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
