from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from sqlalchemy.dialects.postgresql import insert as pg_insert

from database import engine, get_db, Base
import models
import schemas
import auth

Base.metadata.create_all(bind=engine)

app = FastAPI(title="Pedometer Sync API")


@app.post("/register", response_model=schemas.Token, status_code=status.HTTP_201_CREATED)
def register(payload: schemas.UserCreate, db: Session = Depends(get_db)):
    existing = db.query(models.User).filter(models.User.username == payload.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="Username already taken")

    user = models.User(
        username=payload.username,
        password_hash=auth.hash_password(payload.password),
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    token = auth.create_access_token({"sub": user.username})
    return schemas.Token(access_token=token)


@app.post("/login", response_model=schemas.Token)
def login(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.username == form_data.username).first()
    if not user or not auth.verify_password(form_data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
        )
    token = auth.create_access_token({"sub": user.username})
    return schemas.Token(access_token=token)


@app.post("/steps/sync", response_model=list[schemas.StepRecordOut])
def sync_steps(
    payload: schemas.StepSyncRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(auth.get_current_user),
):
    """
    Upserts step counts. The Android client sends the cumulative step
    count for a given device+day; if that day already has a row, the
    value is overwritten (last write wins) rather than duplicated.
    """
    if not payload.entries:
        raise HTTPException(status_code=400, detail="No entries provided")

    results = []
    for entry in payload.entries:
        stmt = (
            pg_insert(models.StepRecord)
            .values(
                user_id=current_user.id,
                device_id=entry.device_id,
                day=entry.day,
                steps=entry.steps,
            )
            .on_conflict_do_update(
                index_elements=["user_id", "device_id", "day"],
                set_={"steps": entry.steps, "synced_at": func_now()},
            )
        )
        db.execute(stmt)
    db.commit()

    records = (
        db.query(models.StepRecord)
        .filter(models.StepRecord.user_id == current_user.id)
        .filter(
            models.StepRecord.device_id.in_([e.device_id for e in payload.entries]),
            models.StepRecord.day.in_([e.day for e in payload.entries]),
        )
        .all()
    )
    return records


@app.get("/steps", response_model=list[schemas.StepRecordOut])
def get_steps(
    db: Session = Depends(get_db),
    current_user: models.User = Depends(auth.get_current_user),
):
    return (
        db.query(models.StepRecord)
        .filter(models.StepRecord.user_id == current_user.id)
        .order_by(models.StepRecord.day.desc())
        .all()
    )


def func_now():
    from sqlalchemy import func
    return func.now()


@app.get("/health")
def health():
    return {"status": "ok"}
