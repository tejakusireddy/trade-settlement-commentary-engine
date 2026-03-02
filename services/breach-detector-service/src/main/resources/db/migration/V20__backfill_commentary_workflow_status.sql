DO
$$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'commentaries'
    ) THEN
        UPDATE settlement_breaches AS breach
        SET status = 'COMMENTARY_APPROVED'
        FROM commentaries AS commentary
        WHERE commentary.breach_id = breach.id
          AND commentary.approved_by IS NOT NULL
          AND breach.status <> 'COMMENTARY_APPROVED';

        UPDATE settlement_breaches AS breach
        SET status = 'COMMENTARY_GENERATED'
        FROM commentaries AS commentary
        WHERE commentary.breach_id = breach.id
          AND commentary.approved_by IS NULL
          AND breach.status = 'PENDING_COMMENTARY';
    END IF;
END;
$$;
