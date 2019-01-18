update project set alignment_decider_bean_name = 'NO_ALIGNMENT' where alignment_decider_bean_name = 'noAlignmentDecider';

update project set alignment_decider_bean_name = 'OTP_ALIGNMENT' where alignment_decider_bean_name = 'defaultOtpAlignmentDecider';

update project set alignment_decider_bean_name = 'PAN_CAN_ALIGNMENT' where alignment_decider_bean_name = 'panCanAlignmentDecider';
